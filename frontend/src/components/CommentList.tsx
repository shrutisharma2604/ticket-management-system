import type { CommentResponse } from "../api/tickets";

export function CommentList({ comments }: { comments: CommentResponse[] }) {
    if (comments.length === 0) {
        return <p>No comments yet.</p>;
    }

    return (
        <ul>
            {comments.map((comment) => (
                <li key={comment.id}>
                    <p>{comment.body}</p>
                    <time dateTime={comment.createdAt}>{comment.createdAt}</time>
                </li>
            ))}
        </ul>
    );
}
