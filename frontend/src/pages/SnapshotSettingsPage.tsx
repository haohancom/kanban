import { FormEvent, useEffect, useState } from "react";
import * as defaultApi from "../api/snapshots";
import { ApiError } from "../api/client";
import { SnapshotRunResult, SnapshotSettings } from "../types";

export interface SnapshotSettingsApi {
  getSettings: () => Promise<SnapshotSettings>;
  updateSettings: (settings: SnapshotSettings) => Promise<SnapshotSettings>;
  runSnapshot: () => Promise<SnapshotRunResult>;
}

interface SnapshotSettingsPageProps {
  api?: SnapshotSettingsApi;
}

const emptySettings: SnapshotSettings = {
  enabled: false,
  cron: "0 0 0 * * *",
  retentionDays: 3,
  outputPath: "backup"
};

export default function SnapshotSettingsPage({ api = defaultApi }: SnapshotSettingsPageProps) {
  const [settings, setSettings] = useState<SnapshotSettings>(emptySettings);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [snapshotFile, setSnapshotFile] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    api
      .getSettings()
      .then((nextSettings) => {
        if (active) {
          setSettings(nextSettings);
        }
      })
      .catch((cause: unknown) => {
        if (active) {
          setError(errorMessage(cause, "无法加载快照设置"));
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [api]);

  async function saveSettings(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setMessage(null);

    try {
      const savedSettings = await api.updateSettings({
        ...settings,
        retentionDays: Math.max(1, settings.retentionDays || 1)
      });
      setSettings(savedSettings);
      setMessage("快照设置已保存");
    } catch (cause: unknown) {
      setError(errorMessage(cause, "快照设置保存失败"));
    } finally {
      setSaving(false);
    }
  }

  async function runSnapshot() {
    setRunning(true);
    setError(null);
    setMessage(null);

    try {
      const result = await api.runSnapshot();
      setSnapshotFile(result.fileName);
    } catch (cause: unknown) {
      setError(errorMessage(cause, "快照生成失败"));
    } finally {
      setRunning(false);
    }
  }

  return (
    <section className="board-shell admin-page" aria-labelledby="snapshot-settings-title">
      <div className="board-toolbar">
        <div>
          <p className="workspace-kicker">系统管理</p>
          <h2 id="snapshot-settings-title">快照设置</h2>
        </div>
        <button type="button" onClick={() => void runSnapshot()} disabled={loading || running}>
          立即生成快照
        </button>
      </div>

      {loading && (
        <p className="board-loading" role="status">
          正在加载快照设置
        </p>
      )}
      {error && <p className="form-error">{error}</p>}
      {message && <p className="form-success">{message}</p>}

      <form className="admin-form" onSubmit={saveSettings}>
        <label className="checkbox-field">
          <input
            type="checkbox"
            checked={settings.enabled}
            onChange={(event) => setSettings({ ...settings, enabled: event.target.checked })}
          />
          <span>启用自动快照</span>
        </label>

        <label className="field">
          <span>Cron 表达式</span>
          <input
            value={settings.cron}
            onChange={(event) => setSettings({ ...settings, cron: event.target.value })}
            required
          />
        </label>

        <label className="field">
          <span>保留天数</span>
          <input
            type="number"
            min={1}
            value={settings.retentionDays}
            onChange={(event) =>
              setSettings({ ...settings, retentionDays: Number(event.target.value) })
            }
            required
          />
        </label>

        <label className="field">
          <span>输出路径</span>
          <input
            value={settings.outputPath}
            onChange={(event) => setSettings({ ...settings, outputPath: event.target.value })}
            required
          />
        </label>

        <div className="form-actions">
          <button type="submit" disabled={loading || saving}>
            保存设置
          </button>
        </div>
      </form>

      {snapshotFile && (
        <div className="result-panel" role="status">
          <span>最新快照</span>
          <strong>{snapshotFile}</strong>
        </div>
      )}
    </section>
  );
}

function errorMessage(cause: unknown, fallback: string) {
  if (cause instanceof ApiError) {
    return cause.message || fallback;
  }
  return fallback;
}
