import { AuthProvider, useAuth } from "./auth/AuthContext";
import LoginPage from "./pages/LoginPage";

function AppContent() {
  const { user, loading, logout } = useAuth();

  if (loading) {
    return (
      <main className="loading-screen" role="status">
        正在加载
      </main>
    );
  }

  if (!user) {
    return <LoginPage />;
  }

  return (
    <main className="workspace">
      <header className="workspace-header">
        <div>
          <p className="workspace-kicker">Kanban</p>
          <h1>工作台</h1>
        </div>
        <div className="current-user">
          <span>{user.displayName}</span>
          <button type="button" onClick={() => void logout()}>
            退出
          </button>
        </div>
      </header>
      <section className="workspace-empty">
        <h2>看板即将就绪</h2>
        <p>请选择团队后查看任务、冲刺和回收站。</p>
      </section>
    </main>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
