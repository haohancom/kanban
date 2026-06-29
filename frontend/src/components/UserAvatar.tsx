interface UserAvatarProps {
  avatarUrl?: string | null;
  displayName: string;
  username: string;
}

export default function UserAvatar({ avatarUrl, displayName, username }: UserAvatarProps) {
  const label = avatarLabel(displayName, username);

  if (avatarUrl) {
    return (
      <img
        alt={`${label} 的头像`}
        className="avatar-circle avatar-image"
        src={avatarUrl}
      />
    );
  }

  return (
    <span className="avatar-circle avatar-default" aria-label={`${label} 的默认头像`}>
      {defaultAvatarInitial(displayName, username)}
    </span>
  );
}

export function defaultAvatarInitial(displayName: string, username: string) {
  const source = displayName.trim() || username.trim();
  return source ? Array.from(source).pop() ?? "?" : "?";
}

function avatarLabel(displayName: string, username: string) {
  return displayName.trim() || username.trim() || "用户";
}
