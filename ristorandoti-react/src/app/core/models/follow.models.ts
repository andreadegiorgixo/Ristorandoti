/** Risposta di POST/DELETE /api/follows/{userId} (FollowStatusDto) */
export interface FollowStatus {
  followedByMe: boolean;
  followersCount: number;
}
