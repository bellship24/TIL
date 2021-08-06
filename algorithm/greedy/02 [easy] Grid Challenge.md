**목차**

- [Link](#link)
- [Problem](#problem)
- [Approach](#approach)
- [First my code](#first-my-code)
- [Refined my code](#refined-my-code)
- [Other's code](#others-code)
- [Note](#note)

---

# Link

- [URL](https://www.hackerrank.com/challenges/grid-challenge/problem)

# Problem

![](/.uploads/2021-08-06-02-21-46.png)
![](/.uploads/2021-08-06-02-21-55.png)

# Approach

문제 요약

- test case 마다 정사각형 격자로 주어진 [a-z] 문자에 대해 row 및 column 방향으로 알파벳 오름차순 정렬되어 있다면 YES 아니면 NO 출력
- row 안에서 재배열 가능
- test case 마다 개행하여 YES or NO 출력할 것
- Input:
  - t: test case
  - n: grid 의 크기 n * n
  - sqaure grid

문제 해석

- test case 로 반복문
  - 일단, row 를 반복문으로 재정렬
  - 그 후, column 을 반복문으로 정렬 확인
  - 모두 정렬 되어 있다면 YES 아니면 NO 출력

예제

로직

- for grid 의 요소
  - 해당 요소 정렬하여 값 업데이트
- for grid[i][j] 에서 i 반복문
  - for grid[i][j] 에서 j 반복문
    - grid[i][j] > grid[i+1][j] 조건문
      - return NO
    - else
      - return YES

로직 적용한 예제

- Input
    - t = 1
    - n = 5
    - grid = ['ebacd', 'fghij', 'olmkn', 'trpqs', 'xywuv']
- for grid 의 요소
  - 'ebacd'
    - grid = ['abcde', 'fghij', 'klmno', 'pqrst', 'uvwxy']
  - grid[0]
    - grid[0][0] < grid[1][0]
    - grid[0][1] < grid[1][1]
    - ...
  - grid[1]
    - ...

# First my code

``` go
func gridChallenge(grid []string) string {
    for i, v := range grid {
        vRuneSlice := strings.Split(v, "")
        sort.Strings(vRuneSlice)
        sortedV := strings.Join(vRuneSlice, "")
        grid[i] = sortedV
    }
    
    for i := range grid[:len(grid)-1] {
        for j := range grid[i] {
            if grid[i][j] > grid[i+1][j] {
                return "NO"
            }
        }
    }
    return "YES"
}
```

# Refined my code

# Other's code

# Note

- grid : 격자