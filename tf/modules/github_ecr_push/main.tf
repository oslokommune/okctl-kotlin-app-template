locals {
  github_account = var.github_account
  github_repo    = coalesce(var.github_repo, var.name)
  ecr_repo       = coalesce(var.ecr_repo, var.name)
  tags           = var.tags
}

data "aws_ecr_repository" "this" {
  name = local.ecr_repo
}

resource "aws_iam_role" "this" {
  name        = "${var.name}-github-ecr-push"
  path        = "/github_actions/"
  description = "Allow pushing images to ECR repo ${local.ecr_repo} from Github Actions in repo ${local.github_account}/${local.github_repo} using OpenID Connect."

  assume_role_policy = data.aws_iam_policy_document.assume_role.json

  inline_policy {
    name   = "${var.name}-github-ecr-push"
    policy = data.aws_iam_policy_document.ecr_upload.json
  }

  managed_policy_arns = []

  tags = local.tags
}

data "aws_iam_policy_document" "assume_role" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    # Condition can also be StringEquals to match a specific branch: "repo:octo-org/octo-repo:ref:refs/heads/octo-branch"
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${local.github_account}/${local.github_repo}:*"]
    }

    principals {
      type        = "Federated"
      identifiers = [var.oidc_provider_arn]
    }
  }
}

data "aws_iam_policy_document" "ecr_upload" {
  statement {
    sid = "AllowUpload"

    actions = [
      "ecr:CompleteLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:InitiateLayerUpload",
      "ecr:BatchCheckLayerAvailability",
      "ecr:PutImage",
    ]

    resources = [
      data.aws_ecr_repository.this.arn,
    ]
  }

  statement {
    sid = "AllowAuth"

    actions = [
      "ecr:GetAuthorizationToken",
    ]

    resources = ["*"]
  }
}
