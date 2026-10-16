create index ix_telemetry_frame_stream
    on telemetry_frame (spacecraft_id, apid, generated_at desc);

create index ix_telemetry_frame_received
    on telemetry_frame (received_at desc);

create index ix_telemetry_sample_series
    on telemetry_sample (parameter_name, generated_at desc);

create index ix_telemetry_sample_frame
    on telemetry_sample (frame_id);

create index ix_alert_open
    on alert (spacecraft_id, acknowledged, raised_at desc);

create index ix_telecommand_dispatch
    on telecommand (spacecraft_id, state, priority desc, submitted_at asc);

create index ix_contact_window_lookup
    on contact_window (spacecraft_id, starts_at, ends_at);

create index ix_audit_event_action
    on audit_event (action, event_sequence desc);
