output "vpc_id" {
  value = data.aws_vpc.existing.id
}

output "vpc_cidr" {
  value = data.aws_vpc.existing.cidr_block
}

output "subnet_id" {
  value = data.aws_subnet.public.id
}

output "subnet_cidr" {
  value = data.aws_subnet.public.cidr_block
}

output "availability_zone" {
  value = data.aws_subnet.public.availability_zone
}