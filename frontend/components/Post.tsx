import React from "react";

const Post = ({ title, description }) => {
  return (
    <div>
      {title}
      <p>{description}</p>

      <button>usun</button>
    </div>
  );
};

export default Post;
