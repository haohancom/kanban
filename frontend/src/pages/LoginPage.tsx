import { FormEvent, useState } from "react";
import { useAuth } from "../auth/AuthContext";

export default function LoginPage() {
  const { error, login } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [loginError, setLoginError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setLoginError(null);

    try {
      await login(username, password);
    } catch {
      setLoginError("用户名或密码不正确");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-panel" aria-labelledby="login-title">
        <p className="login-kicker">Kanban MVP</p>
        <h1 id="login-title">登录工作台</h1>
        <form onSubmit={handleSubmit}>
          <label className="field">
            <span>用户名</span>
            <input
              autoComplete="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
            />
          </label>
          <label className="field">
            <span>密码</span>
            <input
              autoComplete="current-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>
          {(loginError || error) && <p className="form-error">{loginError || error}</p>}
          <button type="submit" disabled={submitting}>
            登录
          </button>
        </form>
      </section>
    </main>
  );
}
