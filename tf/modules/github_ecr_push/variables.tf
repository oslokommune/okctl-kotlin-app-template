variable "name" {
  type        = string
  description = "Used for resource creation. Also used for github/ecr repo names if they are not overriden."
}

variable "oidc_provider_arn" {
  type        = string
  description = "Github OIDC provider ARN"
}

variable "github_account" {
  type        = string
  description = "Owner account of github repo"
  default     = "oslokommune"
}

variable "github_repo" {
  type        = string
  description = "Override value for github repo name"
  default     = ""
}

variable "ecr_repo" {
  type        = string
  description = "Override value for ECR repo name"
  default     = ""
}

variable "tags" {
  type    = map(any)
  default = {}
}
