import React from "react";
import type { PostDto } from "../src/types.ts";

interface PostProps {
  post: PostDto;
  onEdit: (post: PostDto) => void;
  onDelete: (id: number) => void;
}

const Post: React.FC<PostProps> = ({ post, onEdit, onDelete }) => {
  return (
    <article className="card-glass post-card">
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
            <button
              className="btn btn-secondary"
              style={{ padding: "8px 16px", fontSize: "13px" }}
              onClick={() => onEdit(post)}
            >
              Edytuj
            </button>
          )}
          {post.canDelete && (
            <button
              className="btn btn-danger"
              style={{ padding: "8px 16px", fontSize: "13px" }}
              onClick={() => onDelete(post.id)}
            >
              Usuń
            </button>
          )}
        </div>
      )}
    </article>
  );
};

export default Post;
