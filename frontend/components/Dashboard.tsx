import React, { useState, useEffect, useCallback } from "react";
import api from "../src/api.ts";
import Header from "./Header.tsx";
import Post from "./Post.tsx";
import CreatePost from "./CreatePost.tsx";
import AdminPanel from "./AdminPanel.tsx";
import SecurityProfile from "./SecurityProfile.tsx";
import type { User, Team, Role, PostDto } from "../src/types.ts";

interface DashboardProps {
  userProfile: User;
  onLogout: () => void;
}

const Dashboard: React.FC<DashboardProps> = ({ userProfile, onLogout }) => {
  const [posts, setPosts] = useState<PostDto[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  const [editingPost, setEditingPost] = useState<PostDto | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editDescription, setEditDescription] = useState("");

  const [users, setUsers] = useState<User[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);

  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  const isAdmin = userProfile.role.name === "ROLE_ADMIN";

  // fetch posts with pagination
  const fetchPosts = useCallback(async (pageNum: number) => {
    try {
      const res = await api.get<PostDto[]>(`/resources?page=${pageNum}&size=15`);
      const data = res.data;
      
      if (pageNum === 0) {
        setPosts(data);
      } else {
        setPosts((prev) => [...prev, ...data]);
      }

      setHasMore(data.length === 15);
    } catch (err: any) {
      setErrorMsg("blad podczas pobierania postow");
    }
  }, []);

  // fetch management data for admin
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
      console.error("failed to fetch admin panel data", err);
    }
  }, [isAdmin]);

  useEffect(() => {
    fetchPosts(0);
    setPage(0);
    fetchAdminData();
  }, [fetchPosts, fetchAdminData]);

  const handleCreatePost = async (title: string, description: string, isPrivate: boolean) => {
    try {
      await api.post("/resources", { title, description, isPrivate });
      setSuccessMsg("dodano nowy post!");
      setErrorMsg("");
      fetchPosts(0);
      setPage(0);
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || "blad podczas dodawania posta");
    }
  };

  const handleDeletePost = async (id: number) => {
    if (!window.confirm("na pewno chcesz usunac ten post?")) return;
    try {
      await api.delete(`/resources/${id}`);
      setSuccessMsg("post zostal usuniety");
      setPosts((prev) => prev.filter((p) => p.id !== id));
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || "blad podczas usuwania");
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
      setErrorMsg("tytul i tresc nie moga byc puste");
      return;
    }

    try {
      await api.put(`/resources/${editingPost.id}`, {
        title: editTitle,
        description: editDescription,
      });

      setSuccessMsg("zaktualizowano post");
      setEditingPost(null);
      
      setPosts((prev) =>
        prev.map((p) =>
          p.id === editingPost.id
            ? { ...p, title: editTitle, description: editDescription }
            : p
        )
      );
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || "blad zapisu zmian");
    }
  };

  // change user role (admin only)
  const handleRoleChange = async (targetUserId: number, newRole: string) => {
    try {
      await api.put(`/users/${targetUserId}/role`, { role: newRole });
      setSuccessMsg("rola zostala zmieniona");
      fetchAdminData();
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || "blad zmiany roli");
    }
  };

  // change user team (admin only)
  const handleTeamChange = async (targetUserId: number, newTeamId: number) => {
    try {
      await api.put(`/users/${targetUserId}/team`, { teamId: newTeamId });
      setSuccessMsg("klub zostal zmieniony");
      fetchAdminData();
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || "blad zmiany klubu");
    }
  };

  const loadMore = () => {
    const next = page + 1;
    setPage(next);
    fetchPosts(next);
  };

  useEffect(() => {
    if (successMsg || errorMsg) {
      const timer = setTimeout(() => {
        setSuccessMsg("");
        setErrorMsg("");
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [successMsg, errorMsg]);

  return (
    <div className="dashboard-container">
      <Header
        username={userProfile.login}
        roleName={userProfile.role.name}
        teamName={userProfile.team?.name}
        onLogout={onLogout}
      />

      <main className="dashboard-main">
        <div className="dashboard-feed">
          {errorMsg && <div className="alert alert-danger">{errorMsg}</div>}
          {successMsg && <div className="alert alert-success">{successMsg}</div>}

          <CreatePost onCreatePost={handleCreatePost} />

          <div className="feed-header">
            <h3>Aktualności i Zasoby</h3>
          </div>

          {posts.length === 0 ? (
            <div className="card-glass" style={{ textAlign: "center", padding: "40px 20px" }}>
              Brak widocznych postów w systemie.
            </div>
          ) : (
            posts.map((post) => (
              <Post
                key={post.id}
                post={post}
                onEdit={handleOpenEdit}
                onDelete={handleDeletePost}
              />
            ))
          )}

          {hasMore && (
            <div className="pagination-container">
              <button className="btn btn-secondary" onClick={loadMore}>
                Wczytaj więcej postów
              </button>
            </div>
          )}
        </div>

        <div className="sidebar-widgets">
          <SecurityProfile
            userId={userProfile.id}
            username={userProfile.login}
            roleName={userProfile.role.name}
            teamName={userProfile.team?.name}
          />

          {isAdmin && (
            <AdminPanel
              users={users}
              teams={teams}
              roles={roles}
              onRoleChange={handleRoleChange}
              onTeamChange={handleTeamChange}
            />
          )}
        </div>
      </main>

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
