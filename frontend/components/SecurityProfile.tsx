import React from "react";

interface SecurityProfileProps {
  userId: number;
  username: string;
  roleName: string;
  teamName?: string;
}

const SecurityProfile: React.FC<SecurityProfileProps> = ({ userId, username, roleName, teamName }) => {
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
      <h4>Twój Profil Bezpieczeństwa</h4>
      <div style={{ display: "flex", flexDirection: "column", gap: "12px", fontSize: "14px" }}>
        <div>
          <span style={{ color: "var(--text-secondary)" }}>ID Użytkownika:</span>{" "}
          <code>{userId}</code>
        </div>
        <div>
          <span style={{ color: "var(--text-secondary)" }}>Nazwa konta:</span>{" "}
          <strong>{username}</strong>
        </div>
        <div>
          <span style={{ color: "var(--text-secondary)" }}>Poziom uprawnień:</span>{" "}
          <span className={`badge ${getRoleBadgeClass(roleName)}`}>
            {roleName}
          </span>
        </div>
        <div>
          <span style={{ color: "var(--text-secondary)" }}>Grupa / Drużyna:</span>{" "}
          <strong>{teamName || "Brak"}</strong>
        </div>
      </div>
    </div>
  );
};

export default SecurityProfile;
