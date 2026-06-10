export interface User {
  id: number;
  login: string;
  role: { name: string };
  team: { id: number; name: string } | null;
}

export interface Team {
  id: number;
  name: string;
}

export interface Role {
  name: string;
}

export interface PostDto {
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
