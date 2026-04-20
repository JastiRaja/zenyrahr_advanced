import { useEffect, useState } from "react";
import {
  createCandidate,
  getCandidates,
  getJobPostings,
  transitionCandidateStage,
  type Candidate,
  type JobPosting,
} from "../../api/recruitment";
import { useAuth } from "../../contexts/AuthContext";

export default function Candidates() {
  const { user } = useAuth();
  const [candidates, setCandidates] = useState<Candidate[]>([]);
  const [jobs, setJobs] = useState<JobPosting[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>("");
  const [createError, setCreateError] = useState<string>("");
  const [createSuccess, setCreateSuccess] = useState<string>("");
  const [saving, setSaving] = useState(false);
  const [transitioningId, setTransitioningId] = useState<number | null>(null);
  const [lastCreatedCandidateId, setLastCreatedCandidateId] = useState<number | null>(null);
  const [nextStageByCandidateId, setNextStageByCandidateId] = useState<Record<number, string>>({});
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [source, setSource] = useState("");
  const [notes, setNotes] = useState("");
  const [stage, setStage] = useState<
    "APPLIED" | "SHORTLISTED" | "INTERVIEW" | "OFFERED" | "HIRED" | "REJECTED"
  >("APPLIED");
  const [jobPostingId, setJobPostingId] = useState<number | undefined>(undefined);

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
        const [candidateRows, jobRows] = await Promise.all([
          getCandidates(user?.organizationId),
          getJobPostings(user?.organizationId),
        ]);
        setCandidates(candidateRows);
        setJobs(jobRows);
      } catch (err: any) {
        setError(err?.message || "Failed to load candidates");
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [user?.organizationId]);

  if (loading) {
    return <div className="text-slate-600">Loading candidates...</div>;
  }

  const handleCreateCandidate = async (e: React.FormEvent) => {
    e.preventDefault();
    const normalizedName = fullName.trim();
    const normalizedEmail = email.trim();
    const normalizedSource = source.trim();
    const normalizedNotes = notes.trim();
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!normalizedName) {
      setCreateError("Candidate full name is required.");
      return;
    }
    if (normalizedName.length < 2) {
      setCreateError("Candidate name must be at least 2 characters.");
      return;
    }
    if (normalizedName.length > 150) {
      setCreateError("Candidate name must be 150 characters or less.");
      return;
    }
    if (normalizedEmail && !emailPattern.test(normalizedEmail)) {
      setCreateError("Please enter a valid email address.");
      return;
    }
    if (normalizedSource.length > 80) {
      setCreateError("Source must be 80 characters or less.");
      return;
    }
    if (normalizedNotes.length > 2000) {
      setCreateError("Notes must be 2000 characters or less.");
      return;
    }
    try {
      setSaving(true);
      setCreateError("");
      setCreateSuccess("");
      await createCandidate(
        {
          fullName: normalizedName,
          email: normalizedEmail || undefined,
          stage,
          jobPostingId,
          source: normalizedSource || undefined,
          notes: normalizedNotes || undefined,
        },
        user?.organizationId
      );
      const rows = await getCandidates(user?.organizationId);
      setCandidates(rows);
      setLastCreatedCandidateId(rows[0]?.id ?? null);
      setFullName("");
      setEmail("");
      setSource("");
      setNotes("");
      setStage("APPLIED");
      setJobPostingId(undefined);
      setCreateSuccess("Candidate added.");
    } catch (err: any) {
      setCreateError(normalizeErrorMessage(err, "Failed to create candidate"));
    } finally {
      setSaving(false);
    }
  };

  const handleTransitionCandidate = async (candidate: Candidate) => {
    const nextStage = nextStageByCandidateId[candidate.id] || "";
    if (!nextStage) return;
    if (nextStage === candidate.stage) {
      setError("Candidate is already in that stage.");
      return;
    }
    const rejectionReason =
      nextStage === "REJECTED"
        ? window.prompt("Enter rejection reason (required):")?.trim() || ""
        : undefined;
    if (nextStage === "REJECTED" && !rejectionReason) {
      setError("Rejection reason is required to move candidate to REJECTED.");
      return;
    }
    try {
      setTransitioningId(candidate.id);
      setError("");
      await transitionCandidateStage(
        candidate.id,
        {
          stage: nextStage as
            | "APPLIED"
            | "SHORTLISTED"
            | "INTERVIEW"
            | "OFFERED"
            | "HIRED"
            | "REJECTED",
          notes: "Stage updated from recruitment board",
          rejectionReason,
        },
        user?.organizationId
      );
      const rows = await getCandidates(user?.organizationId);
      setCandidates(rows);
      setCreateSuccess("Candidate stage updated.");
    } catch (err: any) {
      setError(normalizeErrorMessage(err, "Failed to transition candidate stage"));
    } finally {
      setTransitioningId(null);
    }
  };

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold text-slate-900">Candidates</h1>
      <form
        onSubmit={handleCreateCandidate}
        className="rounded-md border border-slate-200 bg-white p-4"
      >
        <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-600">
          Add Candidate
        </h2>
        <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-7">
          <input
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            placeholder="Full name"
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <input
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Email"
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <input
            value={source}
            onChange={(e) => setSource(e.target.value)}
            placeholder="Source"
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <input
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Notes"
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <select
            value={stage}
            onChange={(e) =>
              setStage(
                e.target.value as
                  | "APPLIED"
                  | "SHORTLISTED"
                  | "INTERVIEW"
                  | "OFFERED"
                  | "HIRED"
                  | "REJECTED"
              )
            }
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          >
            <option value="APPLIED">APPLIED</option>
            <option value="SHORTLISTED">SHORTLISTED</option>
            <option value="INTERVIEW">INTERVIEW</option>
            <option value="OFFERED">OFFERED</option>
            <option value="HIRED">HIRED</option>
            <option value="REJECTED">REJECTED</option>
          </select>
          <select
            value={jobPostingId?.toString() || ""}
            onChange={(e) =>
              setJobPostingId(e.target.value ? Number(e.target.value) : undefined)
            }
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          >
            <option value="">No job posting</option>
            {jobs.map((job) => (
              <option key={job.id} value={job.id}>
                {job.title}
              </option>
            ))}
          </select>
          <button
            type="submit"
            disabled={saving}
            className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500 disabled:cursor-not-allowed disabled:bg-indigo-300"
          >
            {saving ? "Saving..." : "Add Candidate"}
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
              <th className="px-4 py-2 text-left text-xs font-semibold uppercase text-slate-500">Name</th>
              <th className="px-4 py-2 text-left text-xs font-semibold uppercase text-slate-500">Email</th>
              <th className="px-4 py-2 text-left text-xs font-semibold uppercase text-slate-500">Stage</th>
              <th className="px-4 py-2 text-left text-xs font-semibold uppercase text-slate-500">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200">
            {candidates.length === 0 ? (
              <tr>
                <td colSpan={4} className="px-4 py-4 text-sm text-slate-500">
                  No candidates found yet.
                </td>
              </tr>
            ) : (
              candidates.map((candidate) => (
                <tr
                  key={candidate.id}
                  className={candidate.id === lastCreatedCandidateId ? "bg-emerald-50" : ""}
                >
                  <td className="px-4 py-3 text-sm text-slate-900">{candidate.fullName}</td>
                  <td className="px-4 py-3 text-sm text-slate-700">{candidate.email || "-"}</td>
                  <td className="px-4 py-3 text-sm text-slate-700">
                    {candidate.stage || "APPLIED"}
                    {candidate.rejectionReason ? (
                      <div className="text-xs text-rose-700">Reason: {candidate.rejectionReason}</div>
                    ) : null}
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-700">
                    <div className="flex items-center gap-2">
                      <select
                        value={nextStageByCandidateId[candidate.id] || ""}
                        onChange={(e) =>
                          setNextStageByCandidateId((prev) => ({
                            ...prev,
                            [candidate.id]: e.target.value,
                          }))
                        }
                        className="rounded-md border border-slate-300 px-2 py-1 text-xs"
                      >
                        <option value="">Select stage</option>
                        <option value="SHORTLISTED">SHORTLISTED</option>
                        <option value="INTERVIEW">INTERVIEW</option>
                        <option value="OFFERED">OFFERED</option>
                        <option value="HIRED">HIRED</option>
                        <option value="REJECTED">REJECTED</option>
                      </select>
                      <button
                        type="button"
                        disabled={transitioningId === candidate.id}
                        onClick={() => handleTransitionCandidate(candidate)}
                        className="rounded-md border border-slate-300 px-2 py-1 text-xs hover:bg-slate-50 disabled:cursor-not-allowed disabled:bg-slate-100"
                      >
                        {transitioningId === candidate.id ? "Updating..." : "Update"}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
