/** @type {import('next').NextConfig} */
const nextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  async rewrites() {
    return [
      {
        source: "BACKEND_URL",
        destination: "https://training-ishita-2026.onrender.com",
      },
    ];
  },
};

export default nextConfig;
