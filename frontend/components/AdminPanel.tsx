import React from "react";
import type { User, Team, Role } from "../src/types.ts";

interface AdminPanelProps {
  users: User[];
  teams: Team[];
  roles: Role[];
  onRoleChange: (userId: number, roleName: string) => void;
  onTeamChange: (userId: number, teamId: number) => void;
}

const AdminPanel: React.FC<AdminPanelProps> = ({ users, teams, roles, onRoleChange, onTeamChange }) => {
  const getRoleBadgeClass = (role: string) => {
    switch (role) {
      case "ROLE_ADMIN":
        return "badge-admin";
      case "ROLE_MOD":
        return "badge-mod";
      default:
        return "badge-user";
    }
  };

  return (
    <div className="card-glass widget-card">
      <h4>Zarządzanie Użytkownikami</h4>
      <div className="admin-user-list">
        {users.map((u) => (
          <div key={u.id} className="admin-user-row">
            <div className="admin-user-info">
              <div className="admin-user-username">{u.login}</div>
              <div className="admin-user-meta">
                <span className={`badge ${getRoleBadgeClass(u.role.name)}`} style={{ fontSize: "9px" }}>
                  {u.role.name.replace("ROLE_", "")}
                </span>
                <span className="badge badge-team" style={{ fontSize: "9px" }}>
                  {u.team ? u.team.name : "Brak"}
                </span>
              </div>
            </div>

            <div className="admin-user-actions">
              <select
                className="admin-select"
                value={u.role.name}
                onChange={(e) => onRoleChange(u.id, e.target.value)}
              >
                {roles.map((r) => (
                  <option key={r.name} value={r.name}>
                    {r.name.replace("ROLE_", "")}
                  </option>
                ))}
              </select>

              <select
                className="admin-select"
                value={u.team ? u.team.id : ""}
                onChange={(e) => onTeamChange(u.id, Number(e.target.value))}
              >
                <option value="" disabled>Zmień team</option>
                {teams.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default AdminPanel;
