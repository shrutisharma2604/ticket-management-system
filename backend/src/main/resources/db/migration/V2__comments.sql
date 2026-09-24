CREATE TABLE comments (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    body VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_comments_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_ticket_created_at ON comments (ticket_id, created_at);
