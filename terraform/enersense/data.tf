data "aws_vpc" "existing" {
  id = "vpc-005beda5b9bb2b257"
}

data "aws_subnet" "public" {
  id = "subnet-0502bf2a577fd4461"
}