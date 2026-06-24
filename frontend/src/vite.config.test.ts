import type { UserConfig } from "vite";
import config from "../vite.config";

describe("Vite development server", () => {
  it("proxies API requests to the backend", () => {
    const viteConfig = config as UserConfig;

    expect(viteConfig.server?.proxy).toMatchObject({
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    });
  });
});
