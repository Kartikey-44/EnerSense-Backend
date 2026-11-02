resource "aws_security_group" "enersense" {
  name        = "enersense-backend-sg"
  description = "Security group for EnerSense Spring Boot backend"
  vpc_id      = data.aws_vpc.existing.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["106.192.72.150/32"]
  }

  ingress {
    description = "EnerSense Spring Boot API"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Allow outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "enersense-backend-sg"
  }
}