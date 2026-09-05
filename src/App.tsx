export default function App() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-br from-slate-50 via-white to-zinc-100 p-8">
      <div className="max-w-2xl space-y-8 text-center">
        <div className="inline-flex h-20 w-20 items-center justify-center rounded-3xl bg-gradient-to-br from-emerald-600 to-teal-700 shadow-lg shadow-emerald-200">
          <svg
            className="h-10 w-10 text-white"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={2}
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M12 4a2 2 0 0 1 2 2 2 2 0 0 1-.8 1.6l5.2 4.4h-13l5.2-4.4A2 2 0 0 1 10 6a2 2 0 0 1 2-2z" />
            <path d="M4 14h16" />
            <path d="M4 18h16" />
          </svg>
        </div>
        <div className="space-y-4">
          <h1 className="text-4xl font-semibold tracking-tight text-slate-900">Armadio</h1>
          <p className="text-lg text-slate-600">
            Offline-first wardrobe manager for Android. Privacy-first, 100% offline, zero permissions.
          </p>
          <p className="text-slate-500">
            The Android project source code lives in the <code className="rounded bg-slate-100 px-2 py-1 text-sm">android/</code> directory.
            Open that folder in Android Studio to build and run the app.
          </p>
        </div>

        <div className="grid gap-4 text-left sm:grid-cols-2">
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="mb-2 font-semibold text-slate-900">F0 Foundations</h2>
            <ul className="space-y-2 text-sm text-slate-600">
              <li>Gradle 8.11.1 + AGP 8.7.3</li>
              <li>Room schema v1 with 10 tables</li>
              <li>Hilt + Jetpack Compose + M3</li>
              <li>Zero-permission CI guardrail</li>
            </ul>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="mb-2 font-semibold text-slate-900">How to use</h2>
            <ol className="list-decimal space-y-2 pl-4 text-sm text-slate-600">
              <li>Generate the Gradle wrapper inside <code className="rounded bg-slate-100 px-1 text-xs">android/</code></li>
              <li>Build to generate the Room golden schema</li>
              <li>Commit the generated schema JSON</li>
              <li>Push to a new GitHub repository</li>
            </ol>
          </div>
        </div>

        <div className="rounded-2xl border border-emerald-100 bg-emerald-50 p-6 text-left">
          <h3 className="mb-2 font-semibold text-emerald-900">Create your GitHub repository</h3>
          <pre className="overflow-x-auto rounded-lg bg-slate-900 p-4 text-left text-sm text-slate-50">
{`cd android
git init
git add .
git commit -m "F0: foundations"
gh repo create armadio --public --source=. --push`}
          </pre>
          <p className="mt-2 text-sm text-emerald-800">
            Or create the repo manually on GitHub and run{' '}
            <code className="rounded bg-emerald-100 px-1 text-xs">git remote add origin &lt;url&gt;</code>.
          </p>
        </div>
      </div>
    </div>
  );
}
