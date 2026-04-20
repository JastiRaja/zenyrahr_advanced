import { Link } from "react-router-dom";

export default function Recruitment() {
  return (
    <div className="space-y-4">
      <h1 className="text-3xl font-bold text-slate-900">Recruitment</h1>
      <p className="text-slate-600">
        Recruitment module is enabled for your account. Job postings, candidate
        pipeline, interview scheduling, and offer workflows will be added in
        upcoming slices.
      </p>
      <div className="flex flex-wrap gap-3">
        <Link
          to="/recruitment/jobs"
          className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
        >
          View Jobs
        </Link>
        <Link
          to="/recruitment/candidates"
          className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
        >
          View Candidates
        </Link>
      </div>
    </div>
  );
}
