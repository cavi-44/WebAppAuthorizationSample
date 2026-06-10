import React from "react";

interface HeaderProps {
  username: string;
  roleName: string;
  teamName?: string;
  onLogout: () => void;
}

const Header: React.FC<HeaderProps> = ({ username, roleName, teamName, onLogout }) => {
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
    <header className="dashboard-header">
      <div className="logo-section">
        <h2>
          SHIELD-AUTH <span>Secured v1</span>
        </h2>
      </div>
      <div className="user-profile-widget">
        <div className="user-meta-info">
          <div className="user-meta-name">Zalogowano: {username}</div>
          <div className="user-meta-badges">
            <span className={`badge ${getRoleBadgeClass(roleName)}`}>
              {roleName.replace("ROLE_", "")}
            </span>
            {teamName && <span className="badge badge-team">{teamName}</span>}
          </div>
        </div>
        <button className="btn btn-secondary" onClick={onLogout}>
          Wyloguj
        </button>
      </div>
    </header>
  );
};

export default Header;
