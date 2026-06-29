# User Profile Design

## Goal

Add a self-service personal profile area for every logged-in user. Users can update
their display name, change their password, upload or remove their avatar, and see
their current account identity from one dedicated place.

The avatar upload and removal entry points must move out of the top-right header.
The header should remain a compact identity display with the current avatar,
display name, and logout button.

## Scope

In scope:

- Add a left-sidebar navigation item named `个人资料` for all authenticated users.
- Add a profile page for the current logged-in user.
- Let the current user update their display name.
- Let the current user change their own password.
- Let the current user upload, replace, and remove their own avatar from the
  profile page.
- Keep the current top-right header avatar as display-only.
- Preserve existing super-administrator user management behavior.

Out of scope:

- Changing immutable login usernames.
- Super administrators editing another user's avatar.
- Avatar cropping, resizing, or image editing.
- Moving existing user administration features into the profile page.
- Adding profile details to task cards or member tables.

## Navigation and Layout

Add `profile` to the frontend workspace view model. The sidebar navigation should
show `个人资料` near the primary workspace navigation, visible to every logged-in
user regardless of team role or super-administrator status.

The profile page should use the existing work-focused page style. It should avoid
a marketing-style hero or decorative panel. The first screen should be the usable
profile form itself, organized into clear sections:

- Current identity: avatar preview, username, display name, and account role.
- Avatar: upload or replace image, and remove the uploaded avatar when present.
- Display name: edit and save the name shown in the app.
- Password: enter the new password and confirm it. Regular users must also enter
  their current password. Super administrators changing their own password do not
  need to enter the current password.

The top-right header should continue to render the current avatar and display
name, but it must not include upload or remove controls.

## Backend API

Keep the existing current-user avatar endpoints:

- `PUT /api/users/me/avatar`
- `GET /api/users/me/avatar`
- `DELETE /api/users/me/avatar`

Add current-user profile endpoints under `/api/users/me`:

- `PATCH /api/users/me`: updates the authenticated user's display name and
  returns the refreshed current-user payload.
- `PATCH /api/users/me/password`: changes the authenticated user's password.
  Regular users must provide their current password. Super administrators may
  omit the current password when changing their own password.

Both new endpoints must resolve the target user from the authenticated session
principal. They must not accept a target user id from the request.

## Payloads and Validation

`PATCH /api/users/me` request:

```json
{
  "displayName": "新的显示名"
}
```

Validation:

- `displayName` is required.
- Blank or whitespace-only names return `400 Bad Request`.

`PATCH /api/users/me/password` request for regular users:

```json
{
  "currentPassword": "old-password",
  "newPassword": "new-password"
}
```

`PATCH /api/users/me/password` request for super administrators:

```json
{
  "newPassword": "new-password"
}
```

Validation:

- `newPassword` is required and must be nonblank.
- Regular users must provide a nonblank `currentPassword`.
- Regular users with an incorrect current password receive `403 Forbidden`.
- Super administrators changing their own password do not require
  `currentPassword`.

The existing super-administrator reset endpoint, `PATCH /api/users/{id}/password`,
continues to reset any user's password without the target user's current password.

## Frontend Data Flow

Extend the profile API module with helpers for current-user display-name and
password updates. The avatar helpers can remain in the same current-user profile
API module.

`App` should pass current-user update handlers into `ProfilePage`. Successful
display-name or avatar actions should update auth state immediately with the
returned current-user payload. Successful password changes should show a success
message and clear password fields.

The active view resolver should allow `profile` for every authenticated user.
If users lose access to role-gated admin pages, the app can continue falling
back to `board` for those views; profile should never be role-gated.

## Error Handling

Profile actions should show short inline Chinese feedback near the relevant
section:

- `资料已更新`
- `密码已更新`
- `头像上传失败`
- `无法更新资料`
- `当前密码不正确`
- `无法更新密码`

The profile page should keep the current displayed data stable when an action
fails.

## Testing

Backend tests:

- Authenticated users can update their own display name.
- Blank current-user display names return `400 Bad Request`.
- Regular users must provide the correct current password to change their
  password.
- Regular users cannot change their password with a missing or wrong current
  password.
- Super administrators can change their own password without current password.
- Current-user profile endpoints reject unauthenticated requests.
- Existing super-administrator reset password behavior remains unchanged.

Frontend tests:

- The sidebar shows `个人资料` for ordinary users.
- Selecting `个人资料` renders the profile page.
- The top-right header does not expose upload or remove avatar controls.
- Updating the display name calls the current-user profile API and updates auth
  state.
- Regular-user password changes include the current password.
- Super-administrator password changes can be submitted without current password.
- Avatar upload and removal are available from the profile page and update auth
  state.
- Failed profile actions show inline error messages.

## Implementation Notes

Keep the change incremental. Reuse existing `UserAvatar`, avatar API behavior,
auth state update patterns, form styles, and page layout conventions. Avoid
renaming unrelated user-management concepts or changing role rules outside the
new current-user profile endpoints.
