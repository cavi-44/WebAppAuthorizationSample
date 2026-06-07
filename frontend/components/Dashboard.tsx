import React, { useState, useEffect, useCallback } from "react";
import api from "../src/api.ts";

interface User {
  id: number;
  login: string;
  role: { name: string };
  team: { id: number; nazwa: string } | null;
}

interface Team {
  id: number;
  nazwa: string;
}

interface Role {
  name: string;
}

interface PostDto {
  id: number;
  title: string;
  description: string;
  authorId: number;
  authorLogin: string;
  authorTeamName: string;
  creationDate: string;
  isPrivate: boolean;
  canEdit: boolean;
  canDelete: boolean;
}

interface DashboardProps {
  userProfile: User;
  onLogout: () => void;
}

const Dashboard: React.FC<DashboardProps> = ({ userProfile, onLogout }) => {
  const [posts, setPosts] = useState<PostDto[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  // Forms
  const [newTitle, setNewTitle] = useState("");
  const [newDescription, setNewDescription] = useState("");
  const [newIsPrivate, setNewIsPrivate] = useState(false);

  // Editing Modal
  const [editingPost, setEditingPost] = useState<PostDto | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editDescription, setEditDescription] = useState("");

  // Admin section
  const [users, setUsers] = useState<User[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);

  // Feedback messages
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  const isAdmin = userProfile.role.name === "ROLE_ADMIN";

  const fetchPosts = useCallback(async (pageNum: number) => {
    try {
      const res = await api.get<PostDto[]>(`/resources?page=${pageNum}&size=15`);
      const data = res.data;
      
      if (pageNum === 0) {
        setPosts(data);
      } else {
        setPosts((prev) => [...prev, ...data]);
      }

      if (data.length < 15) {
        setHasMore(false);
      } else {
        setHasMore(true);
      }
    } catch (err: any) {
      setErrorMsg("Błąd podczas pobierania postów.");
    }
  }, []);

  const fetchAdminData = useCallback(async () => {
    if (!isAdmin) return;
    try {
      const [usersRes, teamsRes, rolesRes] = await Promise.all([
        api.get<User[]>("/users"),
        api.get<Team[]>("/teams"),
        api.get<Role[]>("/roles")
      ]);
      setUsers(usersRes.data);
      setTeams(teamsRes.data);
      setRoles(rolesRes.data);
    } catch (err) {
      console.error("Failed to load admin data", err);
    }
  }, [isAdmin]);

  useEffect(() => {
    fetchPosts(0);
    setPage(0);
    fetchAdminData();
  }, [fetchPosts, fetchAdminData]);

  const handleCreatePost = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim() || !newDescription.trim()) {
      setErrorMsg("Tytuł i treść są wymagane.");
      return;
    }

    try {
      await api.post("/resources", {
        title: newTitle,
        description: newDescription,
        isPrivate: newIsPrivate,
      });

      setSuccessMsg("Pomyślnie dodano post!");
      setErrorMsg("");
      setNewTitle("");
      setNewDescription("");
      setNewIsPrivate(false);
      
      // Reload feed
      fetchPosts(0);
      setPage(0);
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || "Błąd podczas tworzenia posta.");
    }
  };

  const handleDeletePost = async (id: number) => {
    if (!window.confirm("Czy na pewno chcesz usunąć ten post?")) return;
    try {
      await api.delete(`/resources/${id}`);
      setSuccessMsg("Post został usunięty.");
      // Remove from state
      setPosts((prev) => prev.filter((p) => p.id !== id));
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || "Błąd podczas usuwania posta.");
    }
  };

  const handleOpenEdit = (post: PostDto) => {
    setEditingPost(post);
    setEditTitle(post.title);
    setEditDescription(post.description);
  };

  const handleUpdatePost = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingPost) return;
    if (!editTitle.trim() || !editDescription.trim()) {
      setErrorMsg("Tytuł i treść nie mogą być puste.");
      return;
    }

    try {
      await api.put(`/resources/${editingPost.id}`, {
        title: editTitle,
        description: editDescription,
      });

      setSuccessMsg("Pomyślnie zaktualizowano post!");
      setEditingPost(null);
      
      // Update in local state
      setPosts((prev) =>
        prev.map((p) =>
          p.id === editingPost.id
            ? { ...p, title: editTitle, description: editDescription }
            : p
        )
      );
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || "Błąd edycji posta.");
    }
  };

  const handleRoleChange = async (targetUserId: number, newRole: string) => {
    try {
      await api.put(`/users/${targetUserId}/role`, { role: newRole });
      setSuccessMsg("Rola użytkownika została zaktualizowana!");
      // Reload admin data
      fetchAdminData();
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || "Błąd zmiany roli.");
    }
  };

  const handleTeamChange = async (targetUserId: number, newTeamId: number) => {
    try {
      await api.put(`/users/${targetUserId}/team`, { teamId: newTeamId });
      setSuccessMsg("Klub użytkownika został zaktualizowany!");
      // Reload admin data
      fetchAdminData();
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || "Błąd zmiany zespołu.");
    }
  };

  const loadMore = () => {
    const next = page + 1;
    setPage(next);
    fetchPosts(next);
  };

  // Clean up alerts automatically
  useEffect(() => {
    if (successMsg || errorMsg) {
      const timer = setTimeout(() => {
        setSuccessMsg("");
        setErrorMsg("");
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [successMsg, errorMsg]);

  const getRoleBadgeClass = (roleName: string) => {
    switch (roleName) {
      case "ROLE_ADMIN":
        return "badge-admin";
      case "ROLE_MOD":
        return "badge-mod";
      default:
        return "badge-user";
    }
  };

  return (
    <div className="dashboard-container">
      {/* Header bar */}
      <header className="dashboard-header">
        <div className="logo-section">
          <h2>
            SHIELD-AUTH <span>Secured v1</span>
          </h2>
        </div>
        <div className="user-profile-widget">
          <div className="user-meta-info">
            <div className="user-meta-name">Zalogowano: {userProfile.login}</div>
            <div className="user-meta-badges">
              <span className={`badge ${getRoleBadgeClass(userProfile.role.name)}`}>
                {userProfile.role.name.replace("ROLE_", "")}
              </span>
              {userProfile.team && (
                <span className="badge badge-team">{userProfile.team.nazwa}</span>
              )}
            </div>
          </div>
          <button className="btn btn-secondary" onClick={onLogout}>
            Wyloguj
          </button>
        </div>
      </header>

      {/* Main dashboard view */}
      <main className="dashboard-main">
        {/* Feed and forms */}
        <div className="dashboard-feed">
          {/* Notifications */}
          {errorMsg && <div className="alert alert-danger">{errorMsg}</div>}
          {successMsg && <div className="alert alert-success">{successMsg}</div>}

          {/* New Post Box */}
          <div className="card-glass create-post-card">
            <form onSubmit={handleCreatePost}>
              <div className="form-group">
                <label className="form-label">Tytuł nowego posta</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="Wpisz chwytliwy tytuł..."
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Treść posta</label>
                <textarea
                  className="form-input"
                  rows={3}
                  placeholder="Podziel się swoimi przemyśleniami..."
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  style={{ resize: "vertical" }}
                />
              </div>
              <div className="toggle-group">
                <input
                  type="checkbox"
                  id="isPrivate"
                  className="toggle-checkbox"
                  checked={newIsPrivate}
                  onChange={(e) => setNewIsPrivate(e.target.checked)}
                />
                <label htmlFor="isPrivate" className="form-label" style={{ margin: 0, cursor: "pointer" }}>
                  Tylko dla mojej grupy (Prywatny)
                </label>
              </div>
              <div style={{ textAlign: "right", marginTop: "15px" }}>
                <button type="submit" className="btn btn-primary">
                  Opublikuj post
                </button>
              </div>
            </form>
          </div>

          <div className="feed-header">
            <h3>Aktualności i Zasoby</h3>
          </div>

          {/* Posts Feed */}
          {posts.length === 0 ? (
            <div className="card-glass" style={{ textAlign: "center", padding: "40px 20px" }}>
              Brak widocznych postów w systemie.
            </div>
          ) : (
            posts.map((post) => (
              <article key={post.id} className="card-glass post-card">
                <div className="post-header">
                  <div>
                    <h4 className="post-title">{post.title}</h4>
                    <div className="post-meta">
                      <span className="post-meta-text">
                        Autor: <strong>{post.authorLogin}</strong>
                      </span>
                      <span className="badge badge-team">{post.authorTeamName}</span>
                      <span className="post-meta-text">
                        • {new Date(post.creationDate).toLocaleString()}
                      </span>
                    </div>
                  </div>
                  <div>
                    {post.isPrivate ? (
                      <span className="badge badge-private">Grupowy</span>
                    ) : (
                      <span className="badge badge-public">Publiczny</span>
                    )}
                  </div>
                </div>

                <div className="post-body">{post.description}</div>

                {(post.canEdit || post.canDelete) && (
                  <div className="post-actions">
                    {post.canEdit && (
                      <button className="btn btn-secondary" style={{ padding: "8px 16px", fontSize: "13px" }} onClick={() => handleOpenEdit(post)}>
                        Edytuj
                      </button>
                    )}
                    {post.canDelete && (
                      <button className="btn btn-danger" style={{ padding: "8px 16px", fontSize: "13px" }} onClick={() => handleDeletePost(post.id)}>
                        Usuń
                      </button>
                    )}
                  </div>
                )}
              </article>
            ))
          )}

          {/* Pagination */}
          {hasMore && (
            <div className="pagination-container">
              <button className="btn btn-secondary" onClick={loadMore}>
                Wczytaj więcej postów
              </button>
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="sidebar-widgets">
          {/* My Security Info */}
          <div className="card-glass widget-card">
            <h4>Twój Profil Bezpieczeństwa</h4>
            <div style={{ display: "flex", flexDirection: "column", gap: "12px", fontSize: "14px" }}>
              <div>
                <span style={{ color: "var(--text-secondary)" }}>ID Użytkownika:</span>{" "}
                <code>{userProfile.id}</code>
              </div>
              <div>
                <span style={{ color: "var(--text-secondary)" }}>Nazwa konta:</span>{" "}
                <strong>{userProfile.login}</strong>
              </div>
              <div>
                <span style={{ color: "var(--text-secondary)" }}>Poziom uprawnień:</span>{" "}
                <span className={`badge ${getRoleBadgeClass(userProfile.role.name)}`}>
                  {userProfile.role.name}
                </span>
              </div>
              <div>
                <span style={{ color: "var(--text-secondary)" }}>Grupa / Drużyna:</span>{" "}
                <strong>{userProfile.team ? userProfile.team.nazwa : "Brak"}</strong>
              </div>
            </div>
          </div>

          {/* Admin panel */}
          {isAdmin && (
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
                          {u.team ? u.team.nazwa : "Brak"}
                        </span>
                      </div>
                    </div>
                    
                    <div className="admin-user-actions">
                      {/* Change Role */}
                      <select
                        className="admin-select"
                        value={u.role.name}
                        onChange={(e) => handleRoleChange(u.id, e.target.value)}
                      >
                        {roles.map((r) => (
                          <option key={r.name} value={r.name}>
                            {r.name.replace("ROLE_", "")}
                          </option>
                        ))}
                      </select>

                      {/* Change Team */}
                      <select
                        className="admin-select"
                        value={u.team ? u.team.id : ""}
                        onChange={(e) => handleTeamChange(u.id, Number(e.target.value))}
                      >
                        <option value="" disabled>Zmień team</option>
                        {teams.map((t) => (
                          <option key={t.id} value={t.id}>
                            {t.nazwa}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </main>

      {/* Edit Post Modal overlay */}
      {editingPost && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>Edytuj zasób #{editingPost.id}</h3>
            <form onSubmit={handleUpdatePost}>
              <div className="form-group">
                <label className="form-label">Tytuł posta</label>
                <input
                  type="text"
                  className="form-input"
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Opis / Treść zasobu</label>
                <textarea
                  className="form-input"
                  rows={4}
                  value={editDescription}
                  onChange={(e) => setEditDescription(e.target.value)}
                  style={{ resize: "vertical" }}
                />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setEditingPost(null)}>
                  Anuluj
                </button>
                <button type="submit" className="btn btn-primary">
                  Zapisz zmiany
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
