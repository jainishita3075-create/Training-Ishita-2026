/** @type {import('next').NextConfig} */
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
