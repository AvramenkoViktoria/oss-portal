-- Set once the "starts within the hour" reminder was queued; cleared when the meeting is rescheduled.
ALTER TABLE meeting ADD COLUMN reminder_sent_at TIMESTAMP WITH TIME ZONE;
