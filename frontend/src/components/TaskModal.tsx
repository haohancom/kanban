import { FormEvent, useEffect, useRef, useState } from "react";
import { BoardTask, TaskFormValues, TaskStatus } from "../types";

interface ModalMember {
  id: number;
  displayName: string;
}

interface ModalSprint {
  id: number;
  name: string;
}

interface TaskModalProps {
  task: BoardTask | null;
  members: ModalMember[];
  sprints: ModalSprint[];
  submitting: boolean;
  deleting: boolean;
  error: string | null;
  onClose: () => void;
  onSubmit: (values: TaskFormValues) => Promise<void>;
  onDelete?: () => Promise<void>;
}

const statusOptions: Array<{ value: TaskStatus; label: string }> = [
  { value: "TODO", label: "待开始" },
  { value: "IN_PROGRESS", label: "进行中" },
  { value: "DONE", label: "已完成" }
];

export default function TaskModal({
  task,
  members,
  sprints,
  submitting,
  deleting,
  error,
  onClose,
  onSubmit,
  onDelete
}: TaskModalProps) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [remarks, setRemarks] = useState("");
  const [risks, setRisks] = useState("");
  const [status, setStatus] = useState<TaskStatus>("TODO");
  const [sprintId, setSprintId] = useState("");
  const [assigneeId, setAssigneeId] = useState("");
  const modalRef = useRef<HTMLElement | null>(null);
  const titleInputRef = useRef<HTMLInputElement | null>(null);
  const onCloseRef = useRef(onClose);

  useEffect(() => {
    setTitle(task?.title ?? "");
    setDescription(task?.description ?? "");
    setRemarks(task?.remarks ?? "");
    setRisks(task?.risks ?? "");
    setStatus(task?.status ?? "TODO");
    setSprintId(task?.sprintId ? String(task.sprintId) : "");
    setAssigneeId(task?.assigneeId ? String(task.assigneeId) : "");
  }, [task]);

  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    titleInputRef.current?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        event.preventDefault();
        onCloseRef.current();
        return;
      }

      if (event.key === "Tab") {
        keepFocusInsideModal(event, modalRef.current);
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      previousFocus?.focus();
    };
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await onSubmit({
      title: title.trim(),
      description,
      remarks,
      risks,
      status,
      sprintId: sprintId === "" ? null : Number(sprintId),
      assigneeId: assigneeId === "" ? null : Number(assigneeId)
    });
  }

  async function handleDelete() {
    if (!onDelete) {
      return;
    }
    await onDelete();
  }

  return (
    <div className="modal-backdrop">
      <section
        className="task-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="task-modal-title"
        ref={modalRef}
      >
        <form onSubmit={handleSubmit}>
          <div className="modal-heading">
            <div>
              <p className="workspace-kicker">{task ? "编辑任务" : "新建任务"}</p>
              <h3 id="task-modal-title">{task ? task.title : "任务"}</h3>
            </div>
            <button type="button" className="icon-text-button" onClick={onClose}>
              关闭
            </button>
          </div>

          {error && <p className="form-error">{error}</p>}

          <label className="field">
            <span>标题</span>
            <input
              ref={titleInputRef}
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              required
            />
          </label>

          <label className="field">
            <span>描述</span>
            <textarea value={description} onChange={(event) => setDescription(event.target.value)} rows={3} />
          </label>

          <label className="field">
            <span>备注</span>
            <textarea value={remarks} onChange={(event) => setRemarks(event.target.value)} rows={3} />
          </label>

          <label className="field">
            <span>风险</span>
            <textarea value={risks} onChange={(event) => setRisks(event.target.value)} rows={3} />
          </label>

          <div className="modal-grid">
            <label className="field">
              <span>状态</span>
              <select value={status} onChange={(event) => setStatus(event.target.value as TaskStatus)}>
                {statusOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label className="field">
              <span>冲刺</span>
              <select value={sprintId} onChange={(event) => setSprintId(event.target.value)}>
                <option value="">未指定</option>
                {sprints.map((sprint) => (
                  <option key={sprint.id} value={sprint.id}>
                    {sprint.name}
                  </option>
                ))}
              </select>
            </label>

            <label className="field">
              <span>负责人</span>
              <select value={assigneeId} onChange={(event) => setAssigneeId(event.target.value)}>
                <option value="">未指定</option>
                {members.map((member) => (
                  <option key={member.id} value={member.id}>
                    {member.displayName}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="modal-actions">
            <button type="button" className="secondary-button" onClick={onClose}>
              取消
            </button>
            {task && onDelete && (
              <button
                type="button"
                className="secondary-button"
                onClick={() => void handleDelete()}
                disabled={deleting || submitting}
              >
                删除
              </button>
            )}
            <button type="submit" disabled={submitting || !title.trim()}>
              保存
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

function keepFocusInsideModal(event: KeyboardEvent, modal: HTMLElement | null) {
  if (!modal) {
    return;
  }

  const focusableElements = Array.from(
    modal.querySelectorAll<HTMLElement>("button, input, select, textarea, [tabindex]:not([tabindex='-1'])")
  ).filter((element) => !element.hasAttribute("disabled"));

  if (focusableElements.length === 0) {
    return;
  }

  const first = focusableElements[0];
  const last = focusableElements[focusableElements.length - 1];

  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}
