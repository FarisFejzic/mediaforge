# Register your SSH public key with AWS
resource "aws_key_pair" "mediaforge" {
  key_name   = "mediaforge-key"
  public_key = file("~/.ssh/id_ed25519.pub")
}

# Security group — the firewall rules
resource "aws_security_group" "mediaforge" {
  name        = "mediaforge-sg"
  description = "MediaForge access"
  vpc_id      = aws_vpc.mediaforge.id

  # SSH — so you can connect to the instance
  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTP — the frontend (Nginx on the instance)
  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # MinIO — presigned URLs   ← ADD THIS BLOCK
  ingress {
    description = "MinIO"
    from_port   = 30900
    to_port     = 30900
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # allow all outbound
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "mediaforge-sg"
  }
}

# Look up the latest Ubuntu 24.04 AMI for eu-north-1
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# The EC2 instance
resource "aws_instance" "mediaforge" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = "m7i-flex.large"
  subnet_id              = aws_subnet.mediaforge.id
  vpc_security_group_ids = [aws_security_group.mediaforge.id]
  key_name               = aws_key_pair.mediaforge.key_name

  root_block_device {
    volume_size = 30   # GB — room for images + data
    volume_type = "gp3"
  }

  tags = {
    Name = "mediaforge"
  }
}

# Output the public IP so we know where to connect
output "instance_public_ip" {
  value = aws_instance.mediaforge.public_ip
}

output "ssh_command" {
  value = "ssh ubuntu@${aws_instance.mediaforge.public_ip}"
}