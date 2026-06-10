import React, { useState } from "react";

interface CreatePostProps {
  onCreatePost: (title: string, description: string, isPrivate: boolean) => Promise<void>;
}

const CreatePost: React.FC<CreatePostProps> = ({ onCreatePost }) => {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [isPrivate, setIsPrivate] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (title.trim() && description.trim()) {
      await onCreatePost(title, description, isPrivate);
      setTitle("");
      setDescription("");
      setIsPrivate(false);
    }
  };

  return (
    <div className="card-glass create-post-card">
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label">Tytuł nowego posta</label>
          <input
            type="text"
            className="form-input"
            placeholder="Wpisz chwytliwy tytuł..."
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
        </div>
        <div className="form-group">
          <label className="form-label">Treść posta</label>
          <textarea
            className="form-input"
            rows={3}
            placeholder="Podziel się swoimi przemyśleniami..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            style={{ resize: "vertical" }}
            required
          />
        </div>
        <div className="toggle-group">
          <input
            type="checkbox"
            id="isPrivate"
            className="toggle-checkbox"
            checked={isPrivate}
            onChange={(e) => setIsPrivate(e.target.checked)}
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
  );
};

export default CreatePost;
