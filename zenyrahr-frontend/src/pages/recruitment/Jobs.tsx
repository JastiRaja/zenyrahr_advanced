import { useEffect, useState } from "react";
import {
  createJobPosting,
  getJobPostings,
  type JobPosting,
} from "../../api/recruitment";
import { useAuth } from "../../contexts/AuthContext";

export default function Jobs() {
  const { user } = useAuth();
  const [jobs, setJobs] = useState<JobPosting[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>("");
  const [createError, setCreateError] = useState<string>("");
  const [createSuccess, setCreateSuccess] = useState<string>("");
  const [saving, setSaving] = useState(false);
  const [lastCreatedJobId, setLastCreatedJobId] = useState<number | null>(null);
  const [title, setTitle] = useState("");
  const [department, setDepartment] = useState("");
  const [description, setDescription] = useState("");
  const [sourceChannel, setSourceChannel] = useState("");
  const [status, setStatus] = useState<"OPEN" | "CLOSED" | "DRAFT">("OPEN");

  const normalizeErrorMessage = (err: any, fallback: string) => {
    const message = String(err?.message || "").trim();
    if (!message) return fallback;
    if (/request failed with status code/i.test(message)) return fallback;
    return message;
  };

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setError("");
        const rows = await getJobPostings(user?.organizationId);
        setJobs(rows);
      } catch (err: any) {
        setError(err?.message || "Failed to load jobs");
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [user?.organizationId]);

  if (loading) {
    return <div className="text-slate-600">Loading jobs...</div>;
  }

  const handleCreateJob = async (e: React.FormEvent) => {
    e.preventDefault();
    const normalizedTitle = title.trim();
    const normalizedDepartment = department.trim();
    const normalizedDescription = description.trim();
    const normalizedSourceChannel = sourceChannel.trim();

    if (!normalizedTitle) {
      setCreateError("Job title is required.");
      return;
    }
    if (normalizedTitle.length < 3) {
      setCreateError("Job title must be at least 3 characters.");
      return;
    }
    if (normalizedTitle.length > 150) {
      setCreateError("Job title must be 150 characters or less.");
      return;
    }
    if (normalizedDepartment.length > 100) {
      setCreateError("Department must be 100 characters or less.");
      return;
    }
    if (normalizedDescription.length > 2000) {
      setCreateError("Description must be 2000 characters or less.");
      return;
    }
    if (normalizedSourceChannel.length > 120) {
      setCreateError("Source channel must be 120 characters or less.");
      return;
    }
    try {
      setSaving(true);
      setCreateError("");
      setCreateSuccess("");
      await createJobPosting(
        {
          title: normalizedTitle,
          department: normalizedDepartment || undefined,
          status,
          description: normalizedDescription || undefined,
          sourceChannel: normalizedSourceChannel || undefined,
        },
        user?.organizationId
      );
      const rows = await getJobPostings(user?.organizationId);
      setJobs(rows);
      setLastCreatedJobId(rows[0]?.id ?? null);
      setTitle("");
      setDepartment("");
      setDescription("");
      setSourceChannel("");
      setStatus("OPEN");
      setCreateSuccess("Job posting created.");
    } catch (err: any) {
      setCreateError(normalizeErrorMessage(err, "Failed to create job posting"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold text-slate-900">Recruitment Jobs</h1>
      <form
        onSubmit={handleCreateJob}
        className="rounded-md border border-slate-200 bg-white p-4"
      >
        <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-600">
          Create Job Posting
        </h2>
        <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-6">
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Job title"
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <input
            value={department}
            onChange={(e) => setDepartment(e.target.value)}
            placeholder="Department"
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <input
            value={sourceChannel}
            onChange={(e) => setSourceChannel(e.target.value)}
            placeholder="Source channel"
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <input
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Description"
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value as "OPEN" | "CLOSED" | "DRAFT")}
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          >
            <option value="OPEN">OPEN</option>
            <option value="DRAFT">DRAFT</option>
            <option value="CLOSED">CLOSED</option>
          </select>
          <button
            type="submit"
            disabled={saving}
            className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500 disabled:cursor-not-allowed disabled:bg-indigo-300"
          >
            {saving ? "Saving..." : "Create Job"}
          </button>
        </div>
        {createError ? (
          <p className="mt-2 text-sm text-rose-700">{createError}</p>
        ) : null}
        {createSuccess ? (
          <p className="mt-2 text-sm text-emerald-700">{createSuccess}</p>
        ) : null}
      </form>
      {error ? (
        <div className="rounded-md border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      ) : null}
      <div className="rounded-md border border-slate-200 bg-white">
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-4 py-2 text-left text-xs font-semibold uppercase text-slate-500">Title</th>
              <th className="px-4 py-2 text-left text-xs font-semibold uppercase text-slate-500">Department</th>
              <th className="px-4 py-2 text-left text-xs font-semibold uppercase text-slate-500">Source</th>
              <th className="px-4 py-2 text-left text-xs font-semibold uppercase text-slate-500">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200">
            {jobs.length === 0 ? (
              <tr>
                <td colSpan={4} className="px-4 py-4 text-sm text-slate-500">
                  No job postings found yet.
                </td>
              </tr>
            ) : (
              jobs.map((job) => (
                <tr
                  key={job.id}
                  className={job.id === lastCreatedJobId ? "bg-emerald-50" : ""}
                >
                  <td className="px-4 py-3 text-sm text-slate-900">{job.title}</td>
                  <td className="px-4 py-3 text-sm text-slate-700">{job.department || "-"}</td>
                  <td className="px-4 py-3 text-sm text-slate-700">{job.sourceChannel || "-"}</td>
                  <td className="px-4 py-3 text-sm text-slate-700">{job.status || "OPEN"}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
