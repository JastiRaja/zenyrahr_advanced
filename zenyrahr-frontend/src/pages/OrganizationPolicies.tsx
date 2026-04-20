import { FormEvent, useEffect, useMemo, useState } from "react";
import { FileText, Download, Eye, Pencil, Trash2, Upload } from "lucide-react";
import api from "../api/axios";
import { useAuth } from "../contexts/AuthContext";

type PolicyDocument = {
  id: number;
  title: string;
  fileName: string;
  fileUrl: string;
  fileSizeBytes?: number | null;
  uploadedByName?: string;
  updatedAt?: string;
};

const toAbsoluteUrl = (rawUrl: string) => {
  if (!rawUrl) return "";
  if (/^https?:\/\//i.test(rawUrl)) return rawUrl;
  const baseUrl = (import.meta.env.VITE_API_BASE_URL_LOCAL || "").replace(/\/+$/, "");
  return `${baseUrl}${rawUrl.startsWith("/") ? "" : "/"}${rawUrl}`;
};

const formatFileSize = (bytes?: number | null) => {
  if (!bytes || bytes <= 0) return "—";
  const kb = bytes / 1024;
  if (kb < 1024) return `${kb.toFixed(1)} KB`;
  return `${(kb / 1024).toFixed(1)} MB`;
};

export default function OrganizationPolicies() {
  const { user } = useAuth();
  const [policies, setPolicies] = useState<PolicyDocument[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [title, setTitle] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [saving, setSaving] = useState(false);
  const [editingPolicyId, setEditingPolicyId] = useState<number | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editFile, setEditFile] = useState<File | null>(null);

  const role = (user?.role || "").toLowerCase();
  const canManage = role === "hr" || role === "org_admin" || role === "zenyrahr_admin";

  const sortedPolicies = useMemo(
    () =>
      [...policies].sort((a, b) => {
        const aTime = a.updatedAt ? new Date(a.updatedAt).getTime() : 0;
        const bTime = b.updatedAt ? new Date(b.updatedAt).getTime() : 0;
        return bTime - aTime;
      }),
    [policies]
  );

  const loadPolicies = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get<PolicyDocument[]>("/api/organization-policies");
      setPolicies(Array.isArray(response.data) ? response.data : []);
    } catch {
      setError("Unable to load policy documents.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPolicies();
  }, []);

  const handleUpload = async (event: FormEvent) => {
    event.preventDefault();
    if (!file) {
      setError("Please select a PDF file.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      const formData = new FormData();
      formData.append("file", file);
      if (title.trim()) {
        formData.append("title", title.trim());
      }
      await api.post("/api/organization-policies", formData);
      setTitle("");
      setFile(null);
      await loadPolicies();
    } catch {
      setError("Failed to upload policy. Ensure file is PDF and try again.");
    } finally {
      setSaving(false);
    }
  };

  const startEdit = (policy: PolicyDocument) => {
    setEditingPolicyId(policy.id);
    setEditTitle(policy.title || "");
    setEditFile(null);
  };

  const cancelEdit = () => {
    setEditingPolicyId(null);
    setEditTitle("");
    setEditFile(null);
  };

  const handleUpdate = async (policyId: number) => {
    setSaving(true);
    setError("");
    try {
      const formData = new FormData();
      if (editTitle.trim()) {
        formData.append("title", editTitle.trim());
      }
      if (editFile) {
        formData.append("file", editFile);
      }
      await api.put(`/api/organization-policies/${policyId}`, formData);
      cancelEdit();
      await loadPolicies();
    } catch {
      setError("Failed to update policy.");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (policyId: number) => {
    const confirmed = window.confirm("Delete this policy document?");
    if (!confirmed) return;
    setSaving(true);
    setError("");
    try {
      await api.delete(`/api/organization-policies/${policyId}`);
      await loadPolicies();
    } catch {
      setError("Failed to delete policy.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4">
      <section className="overflow-hidden rounded-md border border-slate-300 bg-white shadow-sm">
        <div className="bg-gradient-to-r from-sky-700 to-blue-800 px-6 py-5 text-white">
          <h1 className="text-3xl font-bold tracking-tight">Organization Policies</h1>
          <p className="mt-1 text-sm text-sky-50">
            Access policy PDFs published by your HR/Admin for your organization.
          </p>
        </div>
      </section>

      {canManage && (
        <section className="rounded-md border border-slate-300 bg-white p-4 shadow-sm">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-600">
            Upload New Policy
          </h2>
          <form className="grid grid-cols-1 gap-3 md:grid-cols-4" onSubmit={handleUpload}>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-700"
              placeholder="Policy title (optional)"
            />
            <input
              type="file"
              accept="application/pdf,.pdf"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
              className="rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-700"
            />
            <div className="md:col-span-2">
              <button
                type="submit"
                disabled={saving}
                className="inline-flex items-center rounded-md bg-sky-700 px-4 py-2 text-sm font-semibold text-white hover:bg-sky-800 disabled:opacity-60"
              >
                <Upload className="mr-2 h-4 w-4" />
                {saving ? "Uploading..." : "Upload Policy PDF"}
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="rounded-md border border-slate-300 bg-white shadow-sm">
        {error && (
          <div className="m-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {error}
          </div>
        )}

        {loading ? (
          <div className="p-6 text-sm text-slate-500">Loading policy documents...</div>
        ) : sortedPolicies.length === 0 ? (
          <div className="p-10 text-center text-sm text-slate-500">
            No policy documents available yet.
          </div>
        ) : (
          <div className="divide-y divide-slate-200">
            {sortedPolicies.map((policy) => {
              const fileUrl = toAbsoluteUrl(policy.fileUrl);
              const isEditing = editingPolicyId === policy.id;
              return (
                <div key={policy.id} className="flex flex-col gap-3 p-4 md:flex-row md:items-center md:justify-between">
                  <div className="min-w-0 flex-1">
                    {isEditing ? (
                      <div className="flex flex-col gap-2 md:flex-row md:items-center">
                        <input
                          type="text"
                          value={editTitle}
                          onChange={(e) => setEditTitle(e.target.value)}
                          className="rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-700"
                        />
                        <input
                          type="file"
                          accept="application/pdf,.pdf"
                          onChange={(e) => setEditFile(e.target.files?.[0] || null)}
                          className="rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-700"
                        />
                      </div>
                    ) : (
                      <>
                        <p className="truncate text-sm font-semibold text-slate-900">
                          {policy.title || policy.fileName}
                        </p>
                        <p className="mt-1 text-xs text-slate-500">
                          {policy.fileName} • {formatFileSize(policy.fileSizeBytes)} • Uploaded by{" "}
                          {policy.uploadedByName || "—"}
                        </p>
                      </>
                    )}
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    {isEditing ? (
                      <>
                        <button
                          type="button"
                          onClick={() => handleUpdate(policy.id)}
                          disabled={saving}
                          className="rounded-md bg-indigo-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-indigo-700 disabled:opacity-60"
                        >
                          Save
                        </button>
                        <button
                          type="button"
                          onClick={cancelEdit}
                          className="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                        >
                          Cancel
                        </button>
                      </>
                    ) : (
                      <>
                        <a
                          href={fileUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="inline-flex items-center rounded-md border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                        >
                          <Eye className="mr-1 h-3.5 w-3.5" />
                          View
                        </a>
                        <a
                          href={fileUrl}
                          download={policy.fileName}
                          className="inline-flex items-center rounded-md border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                        >
                          <Download className="mr-1 h-3.5 w-3.5" />
                          Download
                        </a>
                        {canManage && (
                          <>
                            <button
                              type="button"
                              onClick={() => startEdit(policy)}
                              className="inline-flex items-center rounded-md border border-indigo-200 bg-indigo-50 px-3 py-1.5 text-xs font-semibold text-indigo-700 hover:bg-indigo-100"
                            >
                              <Pencil className="mr-1 h-3.5 w-3.5" />
                              Edit
                            </button>
                            <button
                              type="button"
                              onClick={() => handleDelete(policy.id)}
                              className="inline-flex items-center rounded-md border border-rose-200 bg-rose-50 px-3 py-1.5 text-xs font-semibold text-rose-700 hover:bg-rose-100"
                            >
                              <Trash2 className="mr-1 h-3.5 w-3.5" />
                              Delete
                            </button>
                          </>
                        )}
                      </>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {!canManage && (
        <p className="text-xs text-slate-500">
          Only HR and organization admins can upload, edit, or delete policy documents.
        </p>
      )}
    </div>
  );
}
