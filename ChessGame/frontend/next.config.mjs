/** @type {import('next').NextConfig} */
const backendUrl = process.env.BACKEND_URL || "http://localhost:8080";

const nextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "https://training-ishita-2026.onrender.com" + "/api/:path*",
      },
    ];
  },
};

export default nextConfig;
