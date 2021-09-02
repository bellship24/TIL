**목차**

- [Link](#link)
- [Problem](#problem)
- [Approach](#approach)
- [Code](#code)
- [Answer](#answer)
- [Discussion](#discussion)

---

# Link

- [URL](https://www.hackerrank.com/challenges/gem-stones/problem?isFullScreen=true)

# Problem

![](/.uploads/2021-08-04-03-18-03.png)
![](/.uploads/2021-08-04-03-18-10.png)

# Approach

문제 요약

- 돌들의 배열이 주어지는데 그 안에는 각 돌의 내장된 미네랄 문자가 값으로 들어있다.
- 특정 미네랄 문자가 모든 돌들에 각 한 번씩 발생했다면, gemstone 이 된다.
- 이 콜렉션에 gemstone 타입의 수를 구해라.

문제 해석

- 주어진 배열의 각 모든 요소 안에 존재하는 특정 문자의 개수를 구해라.

로직

``` txt
- 배열의 첫번째 요소 안에 각 문자들을 key 로 하는 map 인 set 을 만들고 value 는 true
(즉, 이 set 은 arr[0] 의 unique 한 문자들이 담겨 있음)

- set 의 요소들 k 마다
    - arr[1:] 의 요소들 v 마다
        - k 가 v 안에 있다면 다음 v 로 넘어가고
        - 없다면 set 에서 k 를 지우고
        - 다음 k 로 넘어감

- set 에는 arr[1:] 의 모든 요소들 v 에 대해 존재하는 k 들만 남음

- set 의 길이가 gemstone 의 개수이므로 이 값을 리턴
```

예제

``` txt
arr = ['abcdde', 'baccd', 'eeabg']

set = ['a': true, 'b': true, 'c': true, 'd': true, 'e': true]

1. k = a 일 때,
    1-1. v = arr[1] 'baccd' 일 때,
        a 는 'baccd' 안에 있다.
    1-2. v = arr[2] 'eeabg' 일 때,
        a 는 'eeabg' 안에 있다.
2. k = b 일 때,
...
3. k = c 일 때,
    3-1. v = arr[1] 'baccd' 일 때,
        c 는 `baccd` 안에 있다.
    3-2. v = arr[2] 'eeabg' 일 때,
        c 는 'eeabg' 안에 없다.
        c 를 set 에서 삭제
        3. 종료
...
```

# Code

``` go
func gemstones(arr []string) int32 {
    set := make(map[rune]bool)
    for _, v := range arr[0] {
        set[v] = true
    }
    for k := range set {
        for _, v := range(arr[1:]) {
            if strings.ContainsRune(v, k) { continue }
            delete (set, k)
            break
        }
    }
    return int32(len(set))
}
```

# Answer

(동일)

# Discussion

string 을 split 하는 방법

``` go
// package strings
func Split(s, sep string) []string
```

![](/.uploads/2021-08-04-08-38-02.png)

- string 을 join, split 하는 방법

``` go
package main

import (
  "fmt"
  "strings"
)

func main() {
    myArr := []string{"abc", "def", "ghi"}
    myString := strings.Join(myArr, " ")
    fmt.Println(myString)
    mySlice := strings.Split(myString, "")
    fmt.Println(mySlice)
}
/* Output
abc def ghi
[a b c   d e f   g h i]
*/
```

slice 출력

``` go
package main

import (
  "fmt"
)

func main() {
    myArr := []string{"abc", "def", "ghi"}
    fmt.Printf("myArr %v", myArr)
}

/*
myArr [abc def ghi]
*/
```

rune 자료형

- 유니코드(UTF-8) 를 표현하는 타입

map 선언 및 초기화 방법

``` go
myMap := make(map[int]string)
```

주어진 문자열에서 각 문자들의 사용 유무 체크 혹은 유니크값 목록 생성 방법

``` go
package main

import "fmt"

func main() {
    myString := "abcdefg"
    myUniqueMap := make(map[string]bool)

    for _, v := range myString {
        myUniqueMap[v] = True
    }
    fmt.Println(myUniqueMap)
}
/* Output
map[a:true b:true c:true d:true e:true f:true g:true]
*/
```