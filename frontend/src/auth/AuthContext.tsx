import { createContext, ReactNode, useContext, useEffect, useMemo, useState } from "react";
import { ApiError, apiRequest } from "../api/client";
import { CurrentUser } from "../types";

interface AuthContextValue {
  user: CurrentUser | null;
  loading: boolean;
  error: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<CurrentUser>;
  setCurrentUser: (user: CurrentUser) => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    apiRequest<CurrentUser>("/api/auth/me")
      .then((currentUser) => {
        if (active) {
          setUser(currentUser);
        }
      })
      .catch((cause: unknown) => {
        if (!active) {
          return;
        }

        if (cause instanceof ApiError && cause.status === 401) {
          setUser(null);
          return;
        }

        setError("无法加载登录状态");
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  async function login(username: string, password: string) {
    setError(null);
    await apiRequest<CurrentUser>("/api/auth/login", {
      method: "POST",
      body: { username, password }
    });
    await refreshUser();
  }

  async function logout() {
    setError(null);
    try {
      await apiRequest<void>("/api/auth/logout", { method: "POST" });
    } catch (cause: unknown) {
      if (!(cause instanceof ApiError && cause.status === 401)) {
        setError("退出登录失败，请重新登录");
      }
    } finally {
      setUser(null);
    }
  }

  async function refreshUser() {
    const currentUser = await apiRequest<CurrentUser>("/api/auth/me");
    setUser(currentUser);
    return currentUser;
  }

  function setCurrentUser(currentUser: CurrentUser) {
    setUser(currentUser);
  }

  const value = useMemo(
    () => ({ user, loading, error, login, logout, refreshUser, setCurrentUser }),
    [user, loading, error]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
