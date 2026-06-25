package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpBugReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.qa.BugReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.qa.UpdateBugReportRequest;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.BugReportService;
import com.github.farzadsedaghatbin.shipflow.service.DocumentService;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpContentResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * MCP tools for the bug-tracking domain.
 *
 * <p>The {@code add_comment} tool already accepts {@code entityType: BUG_REPORT}, so an agent could
 * comment on a bug it had no way to read. This class closes that asymmetry: read bugs by task,
 * pitch, or cycle; read a single bug; update bug status.
 */
@Component
@RequiredArgsConstructor
public class BugReportMcpTools {

  public static final String TOOL_GET_BUG_REPORTS = "get_bug_reports";
  public static final String TOOL_GET_BUG_REPORT = "get_bug_report";
  public static final String TOOL_GET_BUG_ATTACHMENTS = "get_bug_attachments";
  public static final String TOOL_DOWNLOAD_BUG_ATTACHMENT = "download_bug_attachment";
  public static final String TOOL_UPDATE_BUG_STATUS = "update_bug_status";

  /** Max raw image size returned inline (8 MB) — larger images blow up the context window. */
  static final long MAX_INLINE_IMAGE_BYTES = 8L * 1024 * 1024;

  private final BugReportService bugReportService;
  private final DocumentService documentService;
  private final UserRepository userRepository;

  // ── Tool definitions ──────────────────────────────────────────────────────

  public static Map<String, Object> getBugReportsDefinition() {
    return Map.of(
        "name", TOOL_GET_BUG_REPORTS,
        "description",
            "List bug reports. Filter by projectId, taskId, pitchId, or cycleId — all optional; "
                + "omitting all returns the most-recent 50 bugs across the workspace. "
                + "Use the 'search' param to match title, description, or bugKey. "
                + "Returns bugKey, title, severity (TRIVIAL/MINOR/MAJOR/CRITICAL/BLOCKER), "
                + "status (OPEN, IN_PROGRESS, RESOLVED, VERIFIED, CLOSED, REOPENED, WONT_FIX, DUPLICATE), "
                + "reproduction steps, expected vs actual behaviour, and assignee.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "projectId",
                        Map.of("type", "integer", "description", "All bugs in this project"),
                        "taskId",
                        Map.of("type", "integer", "description", "Bugs linked to this task"),
                        "pitchId",
                        Map.of("type", "integer", "description", "Bugs linked to this pitch"),
                        "cycleId",
                        Map.of("type", "integer", "description", "All bugs in this cycle"),
                        "search",
                        Map.of("type", "string",
                            "description", "Free-text search against title, description, and bugKey")),
                "required", List.of()));
  }

  public static Map<String, Object> getBugReportDefinition() {
    return Map.of(
        "name", TOOL_GET_BUG_REPORT,
        "description",
            "Get full details of a single bug report. Identify it by EITHER its human-facing "
                + "bugKey (the value shown in the UI, e.g. \"BUG-125\") OR its numeric internal "
                + "bugReportId. Prefer bugKey when the user refers to a bug like \"BUG-125\" or "
                + "\"bug 125\" — the key is NOT the same as the numeric ID.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "bugKey",
                        Map.of("type", "string",
                            "description", "Human-facing bug key shown in the UI, e.g. \"BUG-125\""),
                        "bugReportId",
                        Map.of("type", "integer", "description", "Numeric internal bug report ID")),
                "required", List.of()));
  }

  public static Map<String, Object> getBugAttachmentsDefinition() {
    return Map.of(
        "name", TOOL_GET_BUG_ATTACHMENTS,
        "description",
            "List attachments for a bug report. Returns fileName, fileType, fileSize, uploadedBy, "
                + "uploadedAt, and extractedText (text content extracted from PDFs, docs, and "
                + "plain-text files — empty for images and videos). Use this to read log files, "
                + "crash dumps, or any text-based evidence attached to a bug. Identify the bug by "
                + "EITHER its bugKey (e.g. \"BUG-125\") OR its numeric bugReportId. Each attachment "
                + "has a numeric 'id' and an 'isImage' flag — to actually VIEW an image attachment "
                + "(e.g. a design mockup), pass that id to 'download_bug_attachment'.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "bugKey",
                        Map.of("type", "string",
                            "description", "Human-facing bug key shown in the UI, e.g. \"BUG-125\""),
                        "bugReportId",
                        Map.of("type", "integer", "description", "Numeric internal bug report ID")),
                "required", List.of()));
  }

  public static Map<String, Object> downloadBugAttachmentDefinition() {
    return Map.of(
        "name", TOOL_DOWNLOAD_BUG_ATTACHMENT,
        "description",
            "Download an image attachment of a bug report and return it as a viewable image, so you "
                + "can SEE the design mockup / screenshot rather than just its metadata. Pass the "
                + "numeric attachment 'id' from get_bug_attachments. Only image attachments (PNG, "
                + "JPEG, GIF, WebP) are supported — for PDFs/docs use the extractedText from "
                + "get_bug_attachments instead. Images larger than 8 MB are rejected.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "attachmentId",
                        Map.of("type", "integer",
                            "description", "Numeric attachment id from get_bug_attachments")),
                "required", List.of("attachmentId")));
  }

  public static Map<String, Object> updateBugStatusDefinition() {
    return Map.of(
        "name", TOOL_UPDATE_BUG_STATUS,
        "description",
            "Update the status of a bug report. Identify the bug by EITHER its bugKey (e.g. "
                + "\"BUG-125\") OR its numeric bugReportId. Valid statuses: OPEN, IN_PROGRESS, "
                + "RESOLVED, VERIFIED, CLOSED, REOPENED, WONT_FIX, DUPLICATE. Transitioning to "
                + "RESOLVED/VERIFIED/CLOSED stamps the resolvedAt timestamp automatically. Optional "
                + "resolution text captures how it was fixed. Requires WRITE API key scope.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "bugKey",
                        Map.of("type", "string",
                            "description", "Human-facing bug key shown in the UI, e.g. \"BUG-125\""),
                        "bugReportId",
                        Map.of("type", "integer", "description", "Numeric internal ID of the bug"),
                        "status",
                        Map.of(
                            "type", "string",
                            "description", "New bug status",
                            "enum",
                                List.of(
                                    "OPEN",
                                    "IN_PROGRESS",
                                    "RESOLVED",
                                    "VERIFIED",
                                    "CLOSED",
                                    "REOPENED",
                                    "WONT_FIX",
                                    "DUPLICATE")),
                        "resolution",
                        Map.of(
                            "type", "string",
                            "description",
                                "Optional resolution text — what the fix was, why it was closed, "
                                    + "or why it's WONT_FIX/DUPLICATE.")),
                "required", List.of("status")));
  }

  // ── Implementations ──────────────────────────────────────────────────────

  public List<McpBugReportDTO> getBugReports(Map<String, Object> args) {
    Object taskIdArg = args.get("taskId");
    Object pitchIdArg = args.get("pitchId");
    Object cycleIdArg = args.get("cycleId");
    Object projectIdArg = args.get("projectId");
    String search = args.get("search") instanceof String s ? s.trim() : null;

    // Fast-path: single-scope queries that don't need search
    if (search == null || search.isBlank()) {
      if (taskIdArg != null) {
        return bugReportService.getBugReportsByTask(toLong(taskIdArg, "taskId")).stream()
            .map(McpBugReportDTO::from).toList();
      }
      if (pitchIdArg != null) {
        return bugReportService.getBugReportsByPitch(toLong(pitchIdArg, "pitchId")).stream()
            .map(McpBugReportDTO::from).toList();
      }
      if (cycleIdArg != null) {
        return bugReportService.getBugReportsByCycle(toLong(cycleIdArg, "cycleId")).stream()
            .map(McpBugReportDTO::from).toList();
      }
    }

    // Filtered query: supports projectId, cycleId, pitchId, and search
    Long projectId = projectIdArg != null ? toLong(projectIdArg, "projectId") : null;
    Long cycleId = cycleIdArg != null ? toLong(cycleIdArg, "cycleId") : null;
    Long pitchId = pitchIdArg != null ? toLong(pitchIdArg, "pitchId") : null;
    String effectiveSearch = (search != null && !search.isBlank()) ? search : null;

    var pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"));
    return bugReportService
        .getBugReportsWithFilters(projectId, cycleId, pitchId, null, null, null, false,
            effectiveSearch, pageable)
        .getContent()
        .stream()
        .map(McpBugReportDTO::from)
        .toList();
  }

  public McpBugReportDTO getBugReport(Map<String, Object> args) {
    return McpBugReportDTO.from(resolveBug(args));
  }

  public List<Map<String, Object>> getBugAttachments(Map<String, Object> args) {
    long bugId = resolveBugId(args);
    List<UploadedDocument> docs = documentService.getDocumentsByEntity("BUG_REPORT", bugId);
    return docs.stream().map(d -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", d.getId());
      m.put("fileName", d.getOriginalFileName());
      m.put("fileType", d.getFileType());
      m.put("fileSizeBytes", d.getFileSize());
      m.put("uploadedBy", d.getUploaderUsername());
      m.put("uploadedAt", d.getCreatedAt() != null ? d.getCreatedAt().toString() : null);
      m.put("extractedText", d.getExtractedText() != null ? d.getExtractedText() : "");
      m.put("isImage", isImage(d));
      return m;
    }).toList();
  }

  /**
   * Download an image attachment and return it as a viewable MCP image content block. Scoped to bug
   * attachments only (the document must belong to a BUG_REPORT), image types only, and bounded in
   * size.
   */
  public McpContentResult downloadBugAttachment(Map<String, Object> args) {
    long attachmentId = toLong(args.get("attachmentId"), "attachmentId");
    UploadedDocument doc = documentService.getDocumentById(attachmentId);

    if (!"BUG_REPORT".equals(doc.getEntityType())) {
      throw new IllegalArgumentException(
          "Attachment " + attachmentId + " is not a bug report attachment");
    }
    if (!isImage(doc)) {
      throw new IllegalArgumentException(
          "Attachment " + attachmentId + " is not an image (type: " + doc.getFileType()
              + "). For PDFs/docs, use the extractedText from get_bug_attachments instead.");
    }
    if (doc.getFileSize() != null && doc.getFileSize() > MAX_INLINE_IMAGE_BYTES) {
      throw new IllegalArgumentException(
          "Attachment " + attachmentId + " is too large to view inline ("
              + doc.getFileSize() + " bytes; limit is " + MAX_INLINE_IMAGE_BYTES + ").");
    }

    byte[] bytes = documentService.getDocumentBytes(attachmentId);
    String mimeType = documentService.getContentType(doc.getFileType());
    return McpContentResult.imageWithText(bytes, mimeType,
        "Attachment " + attachmentId + " (" + doc.getOriginalFileName() + "):");
  }

  private boolean isImage(UploadedDocument doc) {
    return doc.getFileType() != null
        && documentService.getContentType(doc.getFileType()).startsWith("image/");
  }

  public McpBugReportDTO updateBugStatus(Map<String, Object> args, Authentication auth) {
    long bugReportId = resolveBugId(args);
    String statusStr = (String) args.get("status");
    if (statusStr == null || statusStr.isBlank()) {
      throw new IllegalArgumentException("Missing required argument: status");
    }
    BugStatus status;
    try {
      status = BugStatus.valueOf(statusStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid status '" + statusStr + "'. Must be one of: OPEN, IN_PROGRESS, RESOLVED, "
              + "VERIFIED, CLOSED, REOPENED, WONT_FIX, DUPLICATE");
    }

    User caller = resolveUser(auth);

    UpdateBugReportRequest request = new UpdateBugReportRequest();
    request.setStatus(status);
    if (args.get("resolution") != null) {
      request.setResolution(args.get("resolution").toString());
    }

    BugReportDTO updated = bugReportService.updateBugReport(bugReportId, request, caller.getId());
    return McpBugReportDTO.from(updated);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  /**
   * Resolve a single bug from the tool arguments, accepting either a {@code bugKey} (the
   * human-facing key shown in the UI, e.g. "BUG-125") or a numeric {@code bugReportId}. A bugKey
   * passed in the {@code bugReportId} slot (e.g. the literal string "BUG-125") is also handled, so
   * the agent can't pick the wrong field.
   */
  private BugReportDTO resolveBug(Map<String, Object> args) {
    Object keyArg = args.get("bugKey");
    if (keyArg instanceof String key && !key.isBlank()) {
      return bugReportService.getBugReportByKey(key.trim());
    }
    Object idArg = args.get("bugReportId");
    if (idArg instanceof String s && !s.isBlank() && !isNumeric(s.trim())) {
      return bugReportService.getBugReportByKey(s.trim());
    }
    if (idArg == null) {
      throw new IllegalArgumentException(
          "Provide either bugKey (e.g. \"BUG-125\") or numeric bugReportId");
    }
    return bugReportService.getBugReportById(toLong(idArg, "bugReportId"));
  }

  /**
   * Resolve the numeric bug id from the tool arguments. Unlike {@link #resolveBug}, a numeric
   * {@code bugReportId} is returned directly without a lookup; only a {@code bugKey} requires a
   * service round-trip.
   */
  private long resolveBugId(Map<String, Object> args) {
    Object keyArg = args.get("bugKey");
    if (keyArg instanceof String key && !key.isBlank()) {
      return bugReportService.getBugReportByKey(key.trim()).getId();
    }
    Object idArg = args.get("bugReportId");
    if (idArg instanceof String s && !s.isBlank() && !isNumeric(s.trim())) {
      return bugReportService.getBugReportByKey(s.trim()).getId();
    }
    if (idArg == null) {
      throw new IllegalArgumentException(
          "Provide either bugKey (e.g. \"BUG-125\") or numeric bugReportId");
    }
    return toLong(idArg, "bugReportId");
  }

  private static boolean isNumeric(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (!Character.isDigit(s.charAt(i))) {
        return false;
      }
    }
    return !s.isEmpty();
  }

  private User resolveUser(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new SecurityException("No authenticated user in MCP session");
    }
    return userRepository.findByUsernameWithPerson(auth.getName())
        .orElseThrow(() -> new SecurityException("MCP user not found: " + auth.getName()));
  }

  private static long toLong(Object val, String argName) {
    if (val == null) {
      throw new IllegalArgumentException("Missing required argument: " + argName);
    }
    if (val instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(val.toString());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Argument '" + argName + "' must be a number, got: " + val);
    }
  }
}
