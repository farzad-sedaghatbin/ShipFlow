-- Allow comments on wiki pages.
--
-- V64__add_comments_and_reactions.sql created the `comments` table with a CHECK
-- constraint restricting entity_type to ('TASK', 'BUG_REPORT'). The polymorphic
-- comment system was extended with CommentEntityType.WIKI_PAGE, but inserts with
-- entity_type = 'WIKI_PAGE' fail the original constraint with a data-integrity
-- violation. Drop and recreate the constraint to include the new value.
--
-- (The comment-count triggers in V64 only act on TASK/BUG_REPORT rows, so wiki
-- comments are simply ignored by them — no further change needed.)

ALTER TABLE comments DROP CONSTRAINT IF EXISTS comments_entity_type_check;

ALTER TABLE comments
    ADD CONSTRAINT comments_entity_type_check
    CHECK (entity_type IN ('TASK', 'BUG_REPORT', 'WIKI_PAGE'));
