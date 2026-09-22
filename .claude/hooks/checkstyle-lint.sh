#!/usr/bin/env bash
# PostToolUse(Write|Edit) 훅: 수정된 파일이 .java면 Checkstyle을 돌리고
# 위반 사항을 decision:block JSON으로 Claude에게 돌려준다.
set -u

# stdin으로 들어온 hook JSON에서 tool_input.file_path만 뽑아낸다.
file_path=$(node -e "
let d = '';
process.stdin.on('data', c => d += c);
process.stdin.on('end', () => {
  try {
    const j = JSON.parse(d);
    process.stdout.write((j.tool_input && j.tool_input.file_path) || '');
  } catch (e) {}
});
")

case "$file_path" in
  *.java)
    output=$(./gradlew checkstyleMain --console=plain -q 2>&1)
    exit_code=$?

    if [ $exit_code -ne 0 ]; then
      trimmed="${output:0:4000}"
      node -e "
        process.stdout.write(JSON.stringify({
          decision: 'block',
          reason: 'Checkstyle violations found in ' + process.argv[2] + ':\n\n' + process.argv[1],
        }));
      " "$trimmed" "$file_path"
    fi
    ;;
esac

exit 0
