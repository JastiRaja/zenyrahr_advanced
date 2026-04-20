import axios from "axios";
import api from "./axios";

export interface JobPosting {
  id: number;
  title: string;
  department?: string;
  status?: "OPEN" | "CLOSED" | "DRAFT" | string;
  description?: string;
  sourceChannel?: string;
  ownerEmployeeId?: number;
  openedAt?: string;
  closedAt?: string;
}

export interface Candidate {
  id: number;
  fullName: string;
  email?: string;
  stage?: "APPLIED" | "SHORTLISTED" | "INTERVIEW" | "OFFERED" | "HIRED" | "REJECTED" | string;
  source?: string;
  ownerEmployeeId?: number;
  appliedAt?: string;
  stageUpdatedAt?: string;
  notes?: string;
  rejectionReason?: string;
}

export interface CreateJobPostingRequest {
  title: string;
  department?: string;
  status?: "OPEN" | "CLOSED" | "DRAFT" | string;
  description?: string;
  sourceChannel?: string;
  ownerEmployeeId?: number;
}

export interface CreateCandidateRequest {
  fullName: string;
  email?: string;
  stage?: "APPLIED" | "SHORTLISTED" | "INTERVIEW" | "OFFERED" | "HIRED" | "REJECTED" | string;
  jobPostingId?: number;
  source?: string;
  notes?: string;
  ownerEmployeeId?: number;
}

export interface CandidateStageTransitionRequest {
  stage: "APPLIED" | "SHORTLISTED" | "INTERVIEW" | "OFFERED" | "HIRED" | "REJECTED";
  notes?: string;
  rejectionReason?: string;
}

export class RecruitmentError extends Error {
  constructor(message: string, public statusCode?: number) {
    super(message);
    this.name = "RecruitmentError";
  }
}

export const getJobPostings = async (
  organizationId?: number | null
): Promise<JobPosting[]> => {
  try {
    const response = await api.get("/api/recruitment/jobs", {
      params: organizationId ? { organizationId } : undefined,
    });
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new RecruitmentError(
        error.response.data?.message || "Failed to fetch job postings",
        error.response.status
      );
    }
    throw new RecruitmentError("Failed to fetch job postings");
  }
};

export const getCandidates = async (
  organizationId?: number | null,
  stage?: "APPLIED" | "SHORTLISTED" | "INTERVIEW" | "OFFERED" | "HIRED" | "REJECTED" | "ALL"
): Promise<Candidate[]> => {
  try {
    const params: Record<string, unknown> = {};
    if (organizationId) {
      params.organizationId = organizationId;
    }
    if (stage && stage !== "ALL") {
      params.stage = stage;
    }
    const response = await api.get("/api/recruitment/candidates", {
      params: Object.keys(params).length > 0 ? params : undefined,
    });
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new RecruitmentError(
        error.response.data?.message || "Failed to fetch candidates",
        error.response.status
      );
    }
    throw new RecruitmentError("Failed to fetch candidates");
  }
};

export const createJobPosting = async (
  payload: CreateJobPostingRequest,
  organizationId?: number | null
): Promise<JobPosting> => {
  try {
    const response = await api.post("/api/recruitment/jobs", payload, {
      params: organizationId ? { organizationId } : undefined,
    });
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new RecruitmentError(
        error.response.data?.message || "Failed to create job posting",
        error.response.status
      );
    }
    throw new RecruitmentError("Failed to create job posting");
  }
};

export const createCandidate = async (
  payload: CreateCandidateRequest,
  organizationId?: number | null
): Promise<Candidate> => {
  try {
    const response = await api.post("/api/recruitment/candidates", payload, {
      params: organizationId ? { organizationId } : undefined,
    });
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new RecruitmentError(
        error.response.data?.message || "Failed to create candidate",
        error.response.status
      );
    }
    throw new RecruitmentError("Failed to create candidate");
  }
};

export const transitionCandidateStage = async (
  candidateId: number,
  payload: CandidateStageTransitionRequest,
  organizationId?: number | null
): Promise<Candidate> => {
  try {
    const response = await api.put(`/api/recruitment/candidates/${candidateId}/stage`, payload, {
      params: organizationId ? { organizationId } : undefined,
    });
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new RecruitmentError(
        error.response.data?.message || "Failed to transition candidate stage",
        error.response.status
      );
    }
    throw new RecruitmentError("Failed to transition candidate stage");
  }
};
