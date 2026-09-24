import type { CommentResponse } from "../api/tickets";

export function CommentList({ comments }: { comments: CommentResponse[] }) {
    if (comments.length === 0) {
        return <p className="text-body-secondary">No comments yet.</p>;
    }

    return (
        <ul className="list-group list-group-flush">
            {comments.map((comment) => (
                <li className="list-group-item px-0 py-3" key={comment.id}>
                    <p className="mb-1">{comment.body}</p>
                    <time className="small text-body-secondary" dateTime={comment.createdAt}>
                        {comment.createdAt}
                    </time>
                </li>
            ))}
        </ul>
    );
}
