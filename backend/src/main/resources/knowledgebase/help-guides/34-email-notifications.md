# Email Notifications

ShipFlow can email people about the things that need their attention. Email is **off until your administrator sets up an outgoing mail server**; until then you only get in-app notifications (the bell) and any Slack or Teams alerts.

## What gets emailed
- **You are assigned a task** — an email with the task title and a button that opens it.
- **You are mentioned** — in a comment on a task, bug report or pitch, or in a wiki page.
- **An automation rule tells you something** — rules with the *Notify Assignee* or *Notify Project Members* action email the recipients as well as showing the bell notification. The *Send Email* action emails exactly the people you choose.

You are never emailed about your own actions (mentioning yourself, for example).

## Which address is used
ShipFlow uses the email on your **person profile** (People), or the one on your user account if it has its own. If neither is set, or the address is a placeholder such as `name@shipflow.local`, you simply get no email — nothing bounces. Ask an admin to fix the address on your profile.

## The "Send Email" automation action
In **Automations**, choose the *Send Email* action and set:
- **subject** and **body** — plain text. You can use the same `{{placeholders}}` as other actions (for example `{{cycleName}}`, `{{pitchTitle}}`).
- **to** (optional) — who receives it. Use `project_members` (the default), `project_managers` (members with the Manager role in the project), `assignee` (the task's assignee), or one or more email addresses. Mix them in a list: `["assignee", "boss@yourcompany.com"]`.

A rule that cannot send — the server has no mail set up, or none of the recipients has a usable address — is recorded in the **History** tab as a failure with the reason, so you can see why nothing arrived. A rule can email at most 25 people per run.

## For administrators: setting up the mail server
Set these environment variables for the ShipFlow container, then restart it:

| Variable | Meaning |
|---|---|
| `SMTP_HOST` | Mail server host. Email stays off while this is empty |
| `SMTP_PORT` | Usually `587` (STARTTLS) or `465` (implicit TLS) |
| `SMTP_USER` / `SMTP_PASS` | Login for the sending account |
| `SMTP_FROM` | Sender, e.g. `ShipFlow <shipflow@yourcompany.com>`. Many servers insist this matches the login account |
| `SMTP_REPLY_TO` | Optional. Where replies should go; without it replies go to the sender mailbox |
| `APP_BASE_URL` | Your ShipFlow address (`https://shipflow.yourcompany.com`) so links in emails open the right place |

Every email is marked as automatic (`Auto-Submitted`, `Precedence: bulk`) so out-of-office replies are not sent back to the sender. Send the mail **through your mail server** (not directly from ShipFlow's host) so SPF and DKIM pass and messages do not land in spam. Emails are sent in the background, so a slow mail server never slows the page you are using; a failure is written to the server log as `Failed to send … email`.

To check it works, use **Organization Settings → Email → Send test email**; it emails the address on your own profile.
