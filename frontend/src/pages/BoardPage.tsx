import { useEffect, useMemo, useState } from "react";
import { ApiError } from "../api/client";
import {
  createTask,
  deleteTask,
  listBoardTasks,
  listTeamMembers,
  listTeamSprints,
  updateTask
} from "../api/tasks";
import BoardFilters from "../components/BoardFilters";
import KanbanBoard from "../components/KanbanBoard";
import TaskModal from "../components/TaskModal";
import { BoardTask, BoardTaskFilters, Sprint, TaskFormValues, TaskStatus, Team } from "../types";

interface MemberOption {
  id: number;
  displayName: string;
}

interface TeamMetadata {
  teamId: number;
  members: MemberOption[];
  sprints: Sprint[];
}

interface BoardPageProps {
  canManageSelectedTeam: boolean;
  currentUserId: number;
  selectedTeam: Team | null;
  teamsLoading: boolean;
}

export default function BoardPage({
  canManageSelectedTeam,
  currentUserId,
  selectedTeam,
  teamsLoading
}: BoardPageProps) {
  const [tasks, setTasks] = useState<BoardTask[]>([]);
  const [tasksTeamId, setTasksTeamId] = useState<number | null>(null);
  const [filters, setFilters] = useState<BoardTaskFilters>({});
  const [membersByTeam, setMembersByTeam] = useState<Record<number, MemberOption[]>>({});
  const [sprintsByTeam, setSprintsByTeam] = useState<Record<number, Sprint[]>>({});
  const [boardLoading, setBoardLoading] = useState(false);
  const [boardError, setBoardError] = useState<string | null>(null);
  const [metadataLoading, setMetadataLoading] = useState(false);
  const [metadataError, setMetadataError] = useState<string | null>(null);
  const [editingTask, setEditingTask] = useState<BoardTask | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [savingTask, setSavingTask] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const selectedTeamId = selectedTeam?.id ?? null;
  const metadataTeams = useMemo(
    () => (selectedTeam ? [{ id: selectedTeam.id, name: selectedTeam.name }, ...flattenTeams(selectedTeam.children)] : []),
    [selectedTeam]
  );
  const subTeams = useMemo(() => (selectedTeam ? flattenTeams(selectedTeam.children) : []), [selectedTeam]);
  const filterMembers = useMemo(() => uniqueMembers(Object.values(membersByTeam).flat()), [membersByTeam]);
  const filterSprints = useMemo(() => Object.values(sprintsByTeam).flat(), [sprintsByTeam]);
  const modalTeamId = editingTask?.teamId ?? selectedTeamId;
  const modalMembers = modalTeamId === null ? [] : membersByTeam[modalTeamId] ?? [];
  const modalSprints = modalTeamId === null ? [] : sprintsByTeam[modalTeamId] ?? [];
  const visibleTasks = tasksTeamId === selectedTeamId ? tasks : [];

  useEffect(() => {
    setFilters({});
    setEditingTask(null);
    setModalOpen(false);
  }, [selectedTeamId]);

  useEffect(() => {
    if (!selectedTeamId) {
      setTasks([]);
      setTasksTeamId(null);
      setBoardError(null);
      setBoardLoading(false);
      return;
    }

    let active = true;
    setBoardLoading(true);
    setBoardError(null);

    listBoardTasks(selectedTeamId, filters)
      .then((nextTasks) => {
        if (active) {
          setTasks(nextTasks);
          setTasksTeamId(selectedTeamId);
        }
      })
      .catch((cause: unknown) => {
        if (active) {
          setBoardError(errorMessage(cause, "无法加载任务"));
        }
      })
      .finally(() => {
        if (active) {
          setBoardLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [selectedTeamId, filters, reloadKey]);

  useEffect(() => {
    if (!selectedTeam) {
      setMembersByTeam({});
      setSprintsByTeam({});
      setMetadataError(null);
      setMetadataLoading(false);
      return;
    }

    let active = true;
    setMembersByTeam({});
    setSprintsByTeam({});
    setMetadataLoading(true);
    setMetadataError(null);

    Promise.all(
      metadataTeams.map(async (team) => {
        const [nextMembers, nextSprints] = await Promise.all([
          listTeamMembers(team.id),
          listTeamSprints(team.id)
        ]);
        return {
          teamId: team.id,
          members: nextMembers.map((member) => ({
            id: member.userId,
            displayName: member.displayName
          })),
          sprints: nextSprints
        };
      })
    )
      .then((results) => {
        if (!active) {
          return;
        }
        setMembersByTeam(membersToRecord(results));
        setSprintsByTeam(sprintsToRecord(results));
      })
      .catch((cause: unknown) => {
        if (active) {
          setMembersByTeam({});
          setSprintsByTeam({});
          setMetadataError(errorMessage(cause, "无法加载筛选数据"));
        }
      })
      .finally(() => {
        if (active) {
          setMetadataLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [metadataTeams, selectedTeam]);

  if (teamsLoading) {
    return (
      <section className="workspace-empty" role="status">
        <h2>正在加载团队</h2>
      </section>
    );
  }

  if (!selectedTeam) {
    return (
      <section className="workspace-empty">
        <h2>请选择团队</h2>
        <p>团队创建后，这里会显示对应的任务看板。</p>
      </section>
    );
  }

  async function saveTask(values: TaskFormValues) {
    if (!selectedTeam) {
      return;
    }

    setSavingTask(true);
    setModalError(null);

    try {
      if (editingTask) {
        await updateTask(editingTask.id, values);
      } else {
        await createTask(selectedTeam.id, values);
      }
      setModalOpen(false);
      setEditingTask(null);
      setReloadKey((current) => current + 1);
    } catch (cause: unknown) {
      setModalError(errorMessage(cause, "任务保存失败"));
    } finally {
      setSavingTask(false);
    }
  }

  async function moveTask(task: BoardTask, status: TaskStatus) {
    if (task.status === status) {
      return;
    }

    setBoardError(null);
    try {
      await updateTask(task.id, { status });
      setReloadKey((current) => current + 1);
    } catch (cause: unknown) {
      setBoardError(errorMessage(cause, "任务状态更新失败"));
    }
  }

  async function removeTask(task: BoardTask) {
    setBoardError(null);
    try {
      await deleteTask(task.id);
      setReloadKey((current) => current + 1);
    } catch (cause: unknown) {
      setBoardError(errorMessage(cause, "任务删除失败"));
    }
  }

  function canMoveTask(task: BoardTask) {
    return canManageSelectedTeam || task.assigneeId === currentUserId;
  }

  function openCreateModal() {
    setEditingTask(null);
    setModalError(null);
    setModalOpen(true);
  }

  function openEditModal(task: BoardTask) {
    setEditingTask(task);
    setModalError(null);
    setModalOpen(true);
  }

  return (
    <section className="board-shell" aria-labelledby="board-title">
      <div className="board-toolbar">
        <div>
          <p className="workspace-kicker">当前团队</p>
          <h2 id="board-title">{selectedTeam.name}</h2>
        </div>
        <button type="button" onClick={openCreateModal} disabled={metadataLoading}>
          新建任务
        </button>
      </div>

      <BoardFilters
        teams={subTeams}
        members={filterMembers}
        sprints={filterSprints}
        value={filters}
        onChange={setFilters}
      />

      {metadataError && <p className="form-error">{metadataError}</p>}
      {boardError && <p className="form-error">{boardError}</p>}
      {boardLoading && (
        <p className="board-loading" role="status">
          正在加载任务
        </p>
      )}

      <KanbanBoard
        tasks={visibleTasks}
        canMoveTask={canMoveTask}
        onDelete={canManageSelectedTeam ? removeTask : undefined}
        onEdit={openEditModal}
        onMove={moveTask}
      />

      {modalOpen && (
        <TaskModal
          task={editingTask}
          members={modalMembers}
          sprints={modalSprints}
          submitting={savingTask}
          error={modalError}
          onClose={() => setModalOpen(false)}
          onSubmit={saveTask}
        />
      )}
    </section>
  );
}

function flattenTeams(teams: Team[]): Array<{ id: number; name: string }> {
  return teams.flatMap((team) => [{ id: team.id, name: team.name }, ...flattenTeams(team.children)]);
}

function membersToRecord(results: TeamMetadata[]) {
  return results.reduce<Record<number, MemberOption[]>>((record, result) => {
    record[result.teamId] = result.members;
    return record;
  }, {});
}

function sprintsToRecord(results: TeamMetadata[]) {
  return results.reduce<Record<number, Sprint[]>>((record, result) => {
    record[result.teamId] = result.sprints;
    return record;
  }, {});
}

function uniqueMembers(members: MemberOption[]) {
  const unique = new Map<number, MemberOption>();
  members.forEach((member) => {
    if (!unique.has(member.id)) {
      unique.set(member.id, member);
    }
  });
  return Array.from(unique.values());
}

function errorMessage(cause: unknown, fallback: string) {
  if (cause instanceof ApiError) {
    return cause.message || fallback;
  }
  return fallback;
}
