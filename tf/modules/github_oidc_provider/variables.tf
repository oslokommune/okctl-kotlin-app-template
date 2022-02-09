# For thumbprints value: see `tf/bin/generate_fingerprint_for_oidc.sh`
variable "thumbprints" {
  type        = list(string)
  description = "Github OIDC provider thumbprint"
  default     = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

variable "tags" {
  type    = map(any)
  default = {}
}
