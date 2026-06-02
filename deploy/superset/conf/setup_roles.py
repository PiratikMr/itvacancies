import os
import sys

from superset.app import create_app

STATIC_PERMS = [
    ('can_dashboard',                 'Superset'),
    ('can_explore_json',              'Superset'),
    ('can_fetch_datasource_metadata', 'Superset'),
    ('can_get',                       'Datasource'),
    ('can_list',                      'AsyncEventsRestApi'),
    ('can_query_form_data',           'Api'),
    ('can_read',                      'Chart'),
    ('can_read',                      'Dashboard'),
    ('can_read',                      'DashboardFilterStateRestApi'),
    ('can_write',                     'DashboardFilterStateRestApi'),
    ('can_read',                      'Database'),
    ('can_read',                      'Dataset'),
    ('can_slice',                     'Superset'),
    ('can_time_range',                'Api'),
]

app = create_app()
with app.app_context():
    from superset import security_manager as sm
    from superset.extensions import db

    for name in ('Alpha', 'Gamma', 'sql_lab'):
        role = sm.find_role(name)
        if role:
            db.session.delete(role)
            print(f'[setup_roles] deleted role: {name}')
    db.session.commit()

    public_role = sm.find_role('Public') or sm.add_role('Public')
    public_role.permissions = []
    db.session.commit()

    for perm_name, view_name in STATIC_PERMS:
        pv = sm.find_permission_view_menu(perm_name, view_name)
        if pv:
            sm.add_permission_role(public_role, pv)
            print(f'[setup_roles] + {perm_name} | {view_name}')
        else:
            print(f'[setup_roles] NOT FOUND: {perm_name} | {view_name}', file=sys.stderr)


    PermissionView = sm.permissionview_model
    Permission = sm.permission_model

    pvms = (
        db.session.query(PermissionView)
        .join(Permission, PermissionView.permission_id == Permission.id)
        .filter(Permission.name.in_(['database_access', 'datasource_access']))
        .all()
    )
    for pv in pvms:
        sm.add_permission_role(public_role, pv)
        print(f'[setup_roles] + {pv.permission.name} | {pv.view_menu.name}')

    db.session.commit()
    print(f'[setup_roles] done — Public has {len(public_role.permissions)} permissions')
