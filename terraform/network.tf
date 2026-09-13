# The VPC — an isolated virtual network
resource "aws_vpc" "mediaforge" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags = {
    Name = "mediaforge-vpc"
  }
}

# A subnet within the VPC (public — instances here can have public IPs)
resource "aws_subnet" "mediaforge" {
  vpc_id                  = aws_vpc.mediaforge.id
  cidr_block              = "10.0.1.0/24"
  map_public_ip_on_launch = true
  availability_zone       = "eu-north-1a"
  tags = {
    Name = "mediaforge-subnet"
  }
}

# Internet gateway — lets the VPC reach the internet
resource "aws_internet_gateway" "mediaforge" {
  vpc_id = aws_vpc.mediaforge.id
  tags = {
    Name = "mediaforge-igw"
  }
}

# Route table — routes all outbound traffic through the internet gateway
resource "aws_route_table" "mediaforge" {
  vpc_id = aws_vpc.mediaforge.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.mediaforge.id
  }
  tags = {
    Name = "mediaforge-rt"
  }
}

# Associate the route table with the subnet
resource "aws_route_table_association" "mediaforge" {
  subnet_id      = aws_subnet.mediaforge.id
  route_table_id = aws_route_table.mediaforge.id
}