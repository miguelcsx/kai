{
  description = "Kai Kotlin compiler fuzzing";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.11";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        jdk = pkgs.jdk21;
        gradle = pkgs.gradle_8;
      in
      {
        devShells.default = pkgs.mkShell {
          packages = [
            jdk
            gradle
            pkgs.kotlin
          ];
        };

        packages.default = pkgs.stdenv.mkDerivation {
          pname = "kai";
          version = "0.1.0";
          src = ./.;

          nativeBuildInputs = [
            gradle
            jdk
          ];

          buildPhase = ''
            export GRADLE_USER_HOME=$TMPDIR/gradle-home
            gradle :app:kai-cli:installDist --no-daemon
          '';

          installPhase = ''
            mkdir -p $out
            cp -r app/kai-cli/build/install/kai-cli/* $out/
          '';
        };

        apps.default = {
          type = "app";
          program = "${self.packages.${system}.default}/bin/kai-cli";
        };
      });
}
