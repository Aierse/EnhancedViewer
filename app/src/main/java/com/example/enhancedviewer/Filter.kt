package com.example.enhancedviewer

class Filter {
    private var bracketOpen: Boolean = false
    private var quotaOpen: Boolean = false
    private var duringOpen: Boolean
        get() {
            return bracketOpen || quotaOpen
        }
        set(value: Boolean) {
            bracketOpen = value
            quotaOpen = value
        }

    companion object {
        private val quota: CharArray = charArrayOf('\"', '\'')
        private val bracket: CharArray = charArrayOf('(', '[', '{')
        private val bracketClose: CharArray = charArrayOf(')', ']', '}')
        private val termination: CharArray = charArrayOf('.', ',', '!', '?')
        private val quotaError : Array<String> = arrayOf<String>("\"\"", "''", "'\"", "\"'")

        private fun isQuota(value: Char): Boolean = quota.contains(value)
        private fun isQuotaError(value: String): Boolean = quotaError.contains(value)
        private fun isBracket(value: Char): Boolean = bracket.contains(value)
        private fun isBracketClose(value: Char): Boolean = bracketClose.contains(value)
        private fun isTermination(value: Char): Boolean = termination.contains(value)

        fun deleteGarbageText(value: String): String {
            var temp = value
            temp = temp.replace("‘", "'")
            temp = temp.replace("’", "'")
            temp = temp.replace("“", "\"")
            temp = temp.replace("”", "\"")
            while(temp.contains("  "))
                temp = temp.replace("  ", " ");

            return temp
        }
    }

    private fun deleteAllSpaceBar(value: String): String {
        var temp = value.trim()

        while (temp.contains(" "))
            temp = temp.replace(" ", "")

        return temp
    }

    fun arrangement(text: String): ArrayList<String> {
        val value = text.replace("\\r\\n|\\r|\\n|\\n\\r".toRegex(),"")
        val sb = StringBuilder()
        val temp = arrayListOf<String>()

        for (i in value.indices) {
            sb.append(value[i])

            if (isTermination(value[i])) {
                val nextIndex = i + 1

                if (duringOpen) { // 개행중일 때
                    if (sb.length > 200) {// 개행의 내용이 지나치게 긴 경우 오류의 가능성이 높음
                        // 개행 후 따옴표를 닫지 않아 문장이 길어졌으므로 개행을 이용한 정렬을 포기
                        duringOpen = false
                        temp.add(sb.toString().trim())
                        sb.setLength(0)
                        continue
                    }

                    if (nextIndex < value.length) {
                        val nextChar = value[nextIndex]

                        if (nextChar == ' ' || isQuota(nextChar))
                            continue

                        sb.append(' ')
                        continue
                    }
                }
                else { // 개행중이 아닐 때
                    if (nextIndex < value.length) {
                        val nextChar = value[nextIndex]

                        if (isTermination(nextChar)) // 연속된 ... 일때 넘기기
                            continue

                        if (sb.length == 1) { // 첫글자가 종행문자일 때 버림
                            sb.setLength(0)
                            continue
                        }

                        if (sb.length < 20 && !isQuota(nextChar)) { // 문장의 길이가 짧을 때 다음으로 미룸
                            sb.append(' ')
                            continue
                        }

                        temp.add(sb.toString().trim())
                        sb.setLength(0)
                    }
                }
            }

            else if (isQuota(value[i])) {
                if (!quotaOpen) {
                    quotaOpen = true
                    continue
                }

                if (isQuotaError(deleteAllSpaceBar(sb.toString()))) {
                    //따옴표를 제대로 열거나, 닫지 않아 오류가 발생하고 (닫는 따옴표 - 여는 따옴표)를 연결하여 발생하는 오류 형태
                    //내용을 삭제하고 개행을 유지시켜 오류를 해결
                    sb.setLength(0)
                    sb.append('"')
                    continue
                }

                temp.add(sb.toString().trim())
                sb.setLength(0)
                quotaOpen = false
            }

            else if (isBracket(value[i])) {
                bracketOpen = true
            }
            else if (isBracketClose(value[i])) {
                bracketOpen = false
            }
        }

        temp.add(sb.toString().trim())
        return temp
    }
}