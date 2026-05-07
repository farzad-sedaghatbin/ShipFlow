CREATE UNIQUE INDEX uq_saved_views_default_per_user_project
    ON saved_views(user_id, project_id)
    WHERE is_default = true;
