New tables in the public schema will no longer be exposed to the Data API automatically.

## When this change takes effect

- Starting today (April 28, 2026), you can create new Supabase projects where tables in the `public` schema are not exposed to the Data API and GraphQL API by default. You can enable this setting at project creation.
- On **May 18, 2026**, pg_graphql will not be enabled by default. More details [here](https://github.com/orgs/supabase/discussions/42180).
- On **May 30, 2026** this setting starts to become the default for all new projects. This will be a gradual rollout over a few weeks to monitor impact.
- On **October 30, 2026** the setting will be applied it to all existing projects.

Once the change is rolled out to your project, new tables you create in `public` schema require an explicit opt-in (via a Postgres `grant` ) before the Data API can see them. Existing tables are not affected in your project, they keep their current grants and stay reachable. This change applies to projects that use the **Data API**, Supabase's auto-generated REST and GraphQL layer, which is what `supabase-js` and our other client libraries call. If your app reads and writes Postgres over a direct connection (via psql, ORM or an app server with a connection string), this change will not affect you. 

## What's changing

Previously, when you create a Supabase project with default settings,  `select`, `insert`, `update`, and `delete` are granted to every table in the `public` schema to the `anon`, `authenticated` and `service_role`  roles. Every table you create becomes reachable via the Data API on creation.

From today, the project creation screen includes a setting to opt out of those default grants. When the “Automatically expose new tables” checkbox is unchecked, you opt-in to the new behavior and tables aren’t exposed to the Data API by default. On **May 30, 2026** the opt-out becomes the default for all new projects.

<img width="1350" height="568" alt="CleanShot 2026-05-01 at 15 23 29@2x" src="https://github.com/user-attachments/assets/ca6f82fa-d8a1-439f-bb72-1dc415fa0c33" />

RLS behavior remains unchanged. Grants are a separate layer: they control whether a role can access a table at all, while RLS controls which rows that role can see.

If a grant is missing, PostgREST returns a clear error rather than a silent failure:

```json
{
  "code": "42501",
  "message": "permission denied for table your_table",
  "hint": "Grant the required privileges to the current role with: GRANT SELECT ON public.your_table TO anon;"
}
```

The hint shows you which role is missing which privilege, along with the `GRANT` needed to fix it.

### Before → After

| Step | Before | After |
| --- | --- | --- |
| Create table in `public` | Table is reachable via Data API on creation | Table exists but is **not** reachable via Data API |
| `grant` to `anon` / `authenticated`  / `service_role`  | Implicit, via default privileges | **Required**, explicit `grant` statement |
| `alter table ... enable row level security` | Remains the same | Remains the same |
| `create policy ...` | Remains the same | Remains the same |

## Why

When Supabase launched, a human reviewed each schema change and enabled RLS on new tables as they went. The default grants made this convenient: create a table, and it showed up in your client.

That model doesn’t scale, and it’s easy to accidentally expose new tables before you’ve secured them.Today, agents, CLI scripts, and AI platforms create tables too, and many of those operations do not have a human reviewing the diff.

Explicit grants make access a deliberate, code-level decision.  The PostgREST error response above includes a precise hint, so an agent can self-correct when a grant is missing instead of producing a broken request. 

Without a  `grant`, the API cannot see the table, regardless of how you created it (SQL editor, migrations, Management API, MCP, CLI, or an AI coding tool). Postgres enforces this at the role layer, so the guarantee holds regardless of the creation path.

We’re moving the platform toward declarative code. Explicit Postgres `grant`s are reviewable, diffable, and greppable. They also give you per-role control: `anon` and `authenticated` need different privileges in most schemas, and an explicit grant makes that difference visible in your migrations. This was always possible, now it's the default. 

Currently, the default grants expose every table in `public` over the Data API on creation, including tables a developer forgot to protect.

This is the next step in a series of platform default changes we have shipped over the past quarter:

- a [Data API exposure badge](https://github.com/supabase/supabase/pull/41416) in the Dashboard
- [granular per-table grants](https://github.com/supabase/supabase/pull/42046)
- an [RLS-on-by-default toggle](https://github.com/supabase/supabase/pull/42021)
- the [schema enumeration restriction](https://github.com/orgs/supabase/discussions/42949) in March

## Who is affected

You are unaffected if you only talk to your database over a direct Postgres connection--you can stop reading.

**You need to act** if any of the following is true:

1. Your app reads or writes tables in the `public` schema via the Data API (PostgREST, GraphQL, `supabase-js`, any of the client libraries, or direct HTTP to `/rest/v1/` or `/graphql/v1` )
2. Your migrations or provisioning flow create tables in `public` without explicit `GRANT` statements.
3. Your AI coding tool, CLI script, or Management API call creates tables and expects them to be reachable over the Data API on creation.

The change reaches you on one of three dates:

- **From Today**, if you opt out of the "Automatically expose new tables and functions" setting during project creation.
- **May 30, 2026**, when the new behavior becomes the default for all new projects.
- **October 30, 2026**, when the new behavior is enforced on all existing projects.

Between now and then, Security Advisor will flag affected tables, and we’ll email active projects so you can review access, add grants, and verify your app.

## What to do

For **new tables** you want to expose via the Data API, make explicit grants part of your table-creation flow. Without an explicit `GRANT` , a role will not have access to the created table.

Treat these three steps as a unit. If the grant is missing, Postgres rejects the query before RLS comes into play.

```sql
-- 1. Grant the privileges the role needs
grant select on public.your_table to anon;
grant select, insert, update, delete on public.your_table to authenticated;
grant select, insert, update, delete on public.your_table to service_role;

-- 2. Enable RLS
alter table public.your_table enable row level security;

-- 3. Add the policies you need
create policy "users can read their own rows"
  on public.your_table
  for select
  to authenticated
  using (auth.uid() = user_id);
```

If you use an AI coding tool to create tables, update its system prompt or adopt the [Supabase agent skill](https://supabase.com/blog/supabase-agent-skills), which includes the grants step, and which we keep updated as the platform changes. Skills are easy to install, and handle grants correctly by default. Agents can self-correct from the PostgREST error hint returned as well.

If you run your own migration framework, or a platform that provisions Supabase projects for your own customers, bundle the `GRANT` statements alongside your `ENABLE ROW LEVEL SECURITY` and policy statements in the same migration.

## Opting in on existing projects

You do not have to wait for October 30. To adopt the new behavior on an existing project today, run the same revoke statements the dashboard runs at project creation.

Open the [SQL Editor](https://supabase.com/dashboard/project/_/sql/new) for your project and run:

```sql
-- Stop Postgres from granting default privileges on future objects in public
alter default privileges for role postgres in schema public
  revoke select, insert, update, delete on tables from anon, authenticated, service_role;

alter default privileges for role postgres in schema public
  revoke usage, select on sequences from anon, authenticated, service_role;

```

These four statements change the defaults for **future** tables and sequences that the `postgres` role creates in the `public` schema.

Existing objects keep their current grants, so your running app stays reachable.

From this point on, new tables you create in the `public` schema need explicit `GRANT` statements before the Data API can see them.

If you also want to tighten grants on existing tables you do not want reachable via the Data API, revoke them per table:

```sql
revoke all on table public.your_table from anon, authenticated, service_role;
```

The Data API exposure badge in the Table Editor and the Security Advisor list the tables worth reviewing.

## FAQ

**Does this affect tables in the `storage`, `auth`, `realtime`, or custom schemas?**

No. The change touches default privileges in the `public` schema. Tables in `storage`, `auth`, `realtime`, and any custom schemas you expose via the Data API keep their current grants and their current defaults.

**I opted in on an existing project, created new tables, and my app is broken. How do I roll back?**

Open the [SQL Editor](https://supabase.com/dashboard/project/_/sql/new) and run two blocks.

**1) Restore defaults for future tables**

Future tables will now behave as they did before the revoke:

```sql
alter default privileges for role postgres in schema public
  grant select, insert, update, delete on tables to anon, authenticated, service_role;

alter default privileges for role postgres in schema public
  grant usage, select on sequences to anon, authenticated, service_role;

```

**2) Fix existing tables**

`alter default privileges` only affects future objects, so tables you created since the revoke stay without grants. Grant them in bulk:

```sql
grant select, insert, update, delete on all tables in schema public to anon, authenticated, service_role;
grant usage, select on all sequences in schema public to anon, authenticated, service_role;
```

After running both blocks, your project matches the pre-revoke state. If you added tables you want to keep private, revoke those individually after the bulk grant:

```sql
revoke all on table public.your_private_table from anon, authenticated, service_role;
```

## Rollout timeline

| Date | Milestone | User action |
| --- | --- | --- |
| 2026-04-28 | Changelog published; opt-in toggle available at project creation; docs updated | Try it on a test project; adapt your provisioning flow |
| 2026-05-30 | New behavior becomes the default for all **new** projects | New-project workflows must include explicit `GRANT` statements |
| 2026-10-30 | New behavior enforced on **all existing projects** | Migration must be complete; tables without explicit grants stop being reachable via the Data API |

## Communications timeline

We email owners and admins of every active project, including projects with no Security-Advisor-flagged tables today.

After October 30, any new table created in `public` without an explicit `grant` stops being reachable via the Data API, so a project with no flagged tables today still breaks the first time someone adds a new one.

From today through October 30, the Security Advisor flags affected tables per project and shows the remediation SQL, so you do not have to hunt for them yourself.

| Date | Channel | Audience |
| --- | --- | --- |
| 2026-04-28 | This changelog post | Public |
| 2026-05-07 | April monthly newsletter | All subscribers |
| 2026-05-13 | Email: first notice of the May 30 change for new projects | Owners and admins of all active projects |
| 2026-05-27 | Email: final reminder before the May 30 change for new projects | Owners and admins of all active projects |
| 2026-09-23 | Email: five-week notice before the October 30 change for existing projects | Owners and admins of all active projects |
| 2026-10-23 | Email: final notice, one week before change for existing projects | Owners and admins of all active projects |

If you have a question, create a support ticket [here](https://supabase.com/dashboard/support/new?subject=Breaking%20Change%20-%20Data%20API%20Exposure).