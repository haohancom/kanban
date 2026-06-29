# User Avatar Upload Design

## Goal

Add a self-service avatar feature for logged-in users. Each user can upload,
view, replace, and remove only their own avatar. When no avatar has been
uploaded, the UI shows a default avatar with a blue background and the last
character of the user's visible name.

This is an incremental feature on top of the existing Kanban MVP. The earlier
MVP spec excluded avatars from the initial scope; this document defines the
avatar behavior now being added.

## Scope

In scope:

- Store uploaded avatar binary data in SQLite on the `users` table.
- Let the current logged-in user upload or replace their own avatar.
- Let the current logged-in user remove their uploaded avatar.
- Show the avatar in the workspace header beside the current user's name.
- Fall back to a generated default avatar when no uploaded image exists.
- Keep existing user-administration permissions unchanged.

Out of scope:

- Super administrators uploading avatars for other users.
- Image cropping, resizing, or editing in the browser.
- External object storage or filesystem avatar storage.
- Showing avatars throughout every task/member list in this first increment.

## Data Model

Extend `users` with nullable avatar columns:

- `avatar_data`: uploaded image bytes.
- `avatar_content_type`: MIME type for the uploaded image.
- `avatar_updated_at`: timestamp changed whenever the avatar is uploaded or
  removed.

The schema file should include these columns for new databases. Because the app
already uses `create table if not exists`, the backend also needs a startup
migration that adds missing avatar columns to existing SQLite databases.

Storing avatar bytes in SQLite keeps deployment simple and makes scheduled
database snapshots include avatars automatically.

## Backend API

Add authenticated current-user avatar endpoints:

- `PUT /api/users/me/avatar`: accepts `multipart/form-data` with one `file`
  part. Stores the image for the authenticated user and returns the refreshed
  current user payload.
- `GET /api/users/me/avatar`: returns the uploaded image bytes with the stored
  content type. Returns `404` when the user has no uploaded avatar.
- `DELETE /api/users/me/avatar`: removes the stored avatar and returns the
  refreshed current user payload.

The endpoints must resolve the user from the session principal. They must not
accept a target user id from the request.

## API Payloads

Extend current user and user-account responses with:

- `avatarUrl`: nullable string.

When an avatar exists, `avatarUrl` should point to `/api/users/me/avatar` for
the current user. For user administration rows, the value can remain `null`
unless a later feature needs cross-user avatar display.

The frontend can append a cache-busting query value after upload or deletion so
the browser does not reuse a stale image.

## Validation

The backend accepts only common browser image formats:

- `image/png`
- `image/jpeg`
- `image/webp`
- `image/gif`

The maximum accepted upload size should be 2 MB. Missing files, empty files,
unsupported content types, and oversized files should return `400 Bad Request`.

## Frontend Experience

Create a reusable avatar UI helper/component that can render:

- Uploaded avatar image when `avatarUrl` is present.
- Default avatar when no image is present.

Default avatar behavior:

- Use a blue background.
- Use the last non-whitespace character from `displayName`.
- If `displayName` is blank, use the last non-whitespace character from
  `username`.
- If both are blank, use `?`.

In the workspace header, show the circular avatar next to the current user's
display name. The current user can select an image file from this area. After a
successful upload, update auth state so the new avatar renders immediately.
When an uploaded avatar exists, provide a small remove action that restores the
default avatar.

The upload control should use a native file input for accessibility, with clear
button labels in Chinese.

## Error Handling

Frontend upload errors should show a short inline message near the current-user
area or header, for example:

- `头像上传失败`
- `仅支持 PNG、JPEG、WebP 或 GIF 图片，且不超过 2MB`

The app should preserve the current avatar display if an upload fails.

## Testing

Backend tests:

- Current-user payload includes avatar metadata when an avatar exists.
- Logged-in users can upload, fetch, replace, and delete their own avatar.
- Avatar endpoints reject unauthenticated requests.
- Invalid content types, empty uploads, and oversized uploads return
  `400 Bad Request`.
- Startup migration adds avatar columns for existing SQLite databases.

Frontend tests:

- The workspace header shows the default blue initial avatar from the last
  character of the display name.
- The upload flow calls the current-user avatar API and refreshes auth state.
- Removing an uploaded avatar calls the delete API and restores the default
  avatar.
- Upload failures show an inline error and keep the current display stable.

