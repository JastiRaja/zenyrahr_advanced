import axios from "axios";
import api from "./axios";

export interface JobPosting {
  id: number;
  title: string;
  department?: string;
  status?: "OPEN" | "CLOSED" | "DRAFT" | string;
}

export interface Candidate {
  id: number;
  fullName: string;
  email?: string;
  stage?: "APPLIED" | "SHORTLISTED" | "INTERVIEW" | "OFFERED" | "HIRED" | "REJECTED" | string;
}

export interface CreateJobPostingRequest {
  title: string;
  department?: string;
  status?: "OPEN" | "CLOSED" | "DRAFT" | string;
}

export interface CreateCandidateRequest {
  fullName: string;
  email?: string;
  stage?: "APPLIED" | "SHORTLISTED" | "INTERVIEW" | "OFFERED" | "HIRED" | "REJECTED" | string;
  jobPostingId?: number;
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
  organizationId?: number | null
): Promise<Candidate[]> => {
  try {
    const response = await api.get("/api/recruitment/candidates", {
      params: organizationId ? { organizationId } : undefined,
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
